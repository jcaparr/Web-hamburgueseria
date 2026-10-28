package com.hamburguesas.podio;

import com.hamburguesas.logros.Logro;
import com.hamburguesas.logros.LogroDeUsuario;
import com.hamburguesas.logros.LogroDeUsuarioId;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.logros.Logros;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.service.Bloqueos;
import com.hamburguesas.service.Niveles;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * El podio de personas (#318): quién ganó más XP esta semana, este mes y en total.
 *
 * La XP de un período sale de las reseñas publicadas en él, con el mismo valor de cada una
 * que da el nivel (70 con foto, 100 con comentario), y los 30 de bienvenida cuentan en el
 * período de la primera reseña de la cuenta. Borrar una reseña la saca del período en que
 * se publicó. Semanas (de lunes a domingo) y meses van en hora de Buenos Aires.
 *
 * El empate en XP lo gana quien llegó antes a esa cantidad: el de la última reseña del
 * período más vieja.
 *
 * Lo que se ve se calcula en el momento. Lo que se guarda es el resultado de cada cierre
 * ({@link #cerrarSemana}, {@link #cerrarMes}), que no se recalcula después y da los logros
 * de ranking.
 */
@Service
@RequiredArgsConstructor
public class Podio {

    public static final ZoneId ZONA = Logros.ZONA;
    public static final String SEMANA = "semana";
    public static final String MES = "mes";
    public static final String GENERAL = "general";

    /** Cuántos salen en el top de la pantalla. */
    static final int EN_EL_TOP = 10;

    private final jakarta.persistence.EntityManager em;
    private final UserRepository userRepository;
    private final Bloqueos bloqueos;
    private final ResultadoDePodioRepository resultados;
    private final LogroDeUsuarioRepository logroRepository;

    /** Una reseña como la necesita el podio: de quién, cuándo y cuánta XP da. */
    record Resenia(Long userId, Instant cuando, int xp) {
    }

    /** Lo que ganó una cuenta en un período y cuándo llegó a eso. */
    record Marca(Long userId, int xp, Instant llegoEl) {
    }

    @Transactional(readOnly = true)
    public PodioDto de(String periodo, Long quienMira) {
        List<Marca> ranking;
        Instant cierraEl = null;
        if (GENERAL.equals(periodo)) {
            ranking = rankingGeneral();
        } else {
            Instant[] limites = limites(periodo, LocalDate.now(ZONA));
            ranking = ranking(limites[0], limites[1]);
            cierraEl = limites[1];
        }

        Set<Long> ocultos = Set.copyOf(bloqueos.queNoPuedeVer(quienMira));
        List<Marca> visible = ranking.stream().filter(m -> !ocultos.contains(m.userId())).toList();

        Map<Long, User> personas = userRepository.findAllById(
            visible.stream().limit(EN_EL_TOP).map(Marca::userId).collect(Collectors.toSet()))
            .stream().collect(Collectors.toMap(User::getId, u -> u));
        List<PodioDto.Puesto> top = new ArrayList<>();
        for (int i = 0; i < Math.min(EN_EL_TOP, visible.size()); i++) {
            top.add(puesto(i + 1, visible.get(i), personas.get(visible.get(i).userId()), periodo));
        }

        PodioDto.Puesto yo = null;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).userId().equals(quienMira)) {
                User yoMismo = userRepository.findById(quienMira).orElseThrow();
                yo = puesto(i + 1, visible.get(i), yoMismo, periodo);
            }
        }
        return new PodioDto(periodo, cierraEl, top, yo);
    }

    private PodioDto.Puesto puesto(int puesto, Marca marca, User persona, String periodo) {
        int xpTotal = persona.getXp();
        return new PodioDto.Puesto(puesto, marca.userId(), persona.getUsername(), persona.getHamburguesa(),
            xpTotal, marca.xp());
    }

    /**
     * Cierra la semana que empieza ese lunes: guarda el top 3 y da el Podio semanal. Se puede
     * llamar de nuevo sin efecto: un período ya cerrado no se cierra otra vez.
     */
    @Transactional
    public void cerrarSemana(LocalDate lunes) {
        if (!resultados.de(SEMANA, lunes).isEmpty()) {
            return;
        }
        Instant desde = lunes.atStartOfDay(ZONA).toInstant();
        List<Marca> top = ranking(desde, lunes.plusDays(7).atStartOfDay(ZONA).toInstant()).stream()
            .limit(3).toList();
        guardar(SEMANA, lunes, top);
        top.forEach(m -> otorgar(m.userId(), Logro.PODIO_SEMANAL));
    }

    /**
     * Cierra el mes: guarda el top 3, da el Número uno del mes al primero y la Racha de oro a
     * quien también fue primero el mes anterior.
     */
    @Transactional
    public void cerrarMes(YearMonth mes) {
        LocalDate desdeFecha = mes.atDay(1);
        if (!resultados.de(MES, desdeFecha).isEmpty()) {
            return;
        }
        List<Marca> top = ranking(desdeFecha.atStartOfDay(ZONA).toInstant(),
            mes.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant()).stream().limit(3).toList();
        guardar(MES, desdeFecha, top);
        if (top.isEmpty()) {
            return;
        }
        Long primero = top.get(0).userId();
        otorgar(primero, Logro.PRIMERO_DEL_MES);
        boolean eraPrimeroAntes = resultados.de(MES, mes.minusMonths(1).atDay(1)).stream()
            .anyMatch(r -> r.getId().puesto() == 1 && r.getUserId().equals(primero));
        if (eraPrimeroAntes) {
            otorgar(primero, Logro.RACHA_DE_ORO);
        }
    }

    private void guardar(String periodo, LocalDate desde, List<Marca> top) {
        for (int i = 0; i < top.size(); i++) {
            resultados.save(new ResultadoDePodio(new ResultadoDePodio.Id(periodo, desde, i + 1),
                top.get(i).userId(), top.get(i).xp()));
        }
    }

    /** Un logro de ranking es de los que dan otros: se gana una vez y queda. */
    private void otorgar(Long userId, Logro logro) {
        Instant ahora = Instant.now();
        LogroDeUsuarioId id = new LogroDeUsuarioId(userId, logro.name());
        LogroDeUsuario fila = logroRepository.findById(id).orElse(null);
        if (fila == null) {
            logroRepository.save(new LogroDeUsuario(id, ahora, true, ahora, null, false, null));
        } else if (!fila.isActivo()) {
            fila.setActivo(true);
            fila.setCambioEl(ahora);
        }
    }

    /**
     * Los límites del período que contiene {@code hoy}: [desde, hasta). Semana: de lunes a
     * lunes. Mes: del 1 al 1. El General no tiene: va por {@link #rankingGeneral}.
     */
    static Instant[] limites(String periodo, LocalDate hoy) {
        return switch (periodo) {
            case SEMANA -> {
                LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new Instant[] {lunes.atStartOfDay(ZONA).toInstant(), lunes.plusDays(7).atStartOfDay(ZONA).toInstant()};
            }
            case MES -> {
                YearMonth mes = YearMonth.from(hoy);
                yield new Instant[] {mes.atDay(1).atStartOfDay(ZONA).toInstant(),
                    mes.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant()};
            }
            default -> throw new IllegalArgumentException("Período desconocido: " + periodo);
        };
    }

    /**
     * El ranking de un período, de más a menos XP, solo con quienes ganaron algo. La XP de
     * cada reseña es la que corresponde a su comentario; los 30 de bienvenida van en el
     * período de la primera reseña de la cuenta.
     *
     * Solo se leen las reseñas del período (con su fecha, sin fotos ni locales) y, de cada
     * cuenta, la fecha de su primera reseña: no hace falta traer todas las de la base.
     */
    List<Marca> ranking(Instant desde, Instant hasta) {
        List<Resenia> delPeriodo = em.createQuery(
                "select r.user.id, r.createdAt, r.comment from Rating r "
                    + "where r.createdAt >= :desde and r.createdAt < :hasta "
                    + "order by r.user.id, r.createdAt, r.id", Object[].class)
            .setParameter("desde", desde)
            .setParameter("hasta", hasta)
            .getResultList().stream()
            .map(f -> new Resenia((Long) f[0], (Instant) f[1], Niveles.xpDe((String) f[2])))
            .toList();
        if (delPeriodo.isEmpty()) {
            return List.of();
        }

        Map<Long, Instant> primeraDeCadaCuenta = new HashMap<>();
        em.createQuery("select r.user.id, min(r.createdAt) from Rating r group by r.user.id", Object[].class)
            .getResultList().forEach(f -> primeraDeCadaCuenta.put((Long) f[0], (Instant) f[1]));

        Map<Long, Integer> xp = new HashMap<>();
        Map<Long, Instant> llego = new HashMap<>();
        Long anterior = null;
        for (Resenia r : delPeriodo) {
            // La primera del período es la primera de la cuenta si tiene esa misma fecha: una
            // reseña más vieja de la cuenta caería fuera del período y la fecha sería menor.
            boolean primeraDelPeriodo = !r.userId().equals(anterior);
            anterior = r.userId();
            boolean bienvenida = primeraDelPeriodo && r.cuando().equals(primeraDeCadaCuenta.get(r.userId()));
            xp.merge(r.userId(), r.xp() + (bienvenida ? Niveles.XP_DE_BIENVENIDA : 0), Integer::sum);
            llego.merge(r.userId(), r.cuando(), (a, b) -> b.isAfter(a) ? b : a);
        }
        return ordenado(xp, llego);
    }

    /**
     * El general: la XP que la cuenta ya tiene guardada, que es la que decide el nivel, y la
     * fecha de su última reseña para el desempate. No recorre las reseñas una por una.
     */
    List<Marca> rankingGeneral() {
        Map<Long, Integer> xp = new HashMap<>();
        Map<Long, Instant> llego = new HashMap<>();
        em.createQuery("select u.id, u.xp, (select max(r.createdAt) from Rating r where r.user = u) "
                + "from User u where u.xp > 0", Object[].class)
            .getResultList().forEach(f -> {
                xp.put((Long) f[0], (Integer) f[1]);
                llego.put((Long) f[0], f[2] == null ? Instant.EPOCH : (Instant) f[2]);
            });
        return ordenado(xp, llego);
    }

    private static List<Marca> ordenado(Map<Long, Integer> xp, Map<Long, Instant> llego) {
        return xp.entrySet().stream()
            .map(e -> new Marca(e.getKey(), e.getValue(), llego.get(e.getKey())))
            .sorted(Comparator.comparingInt(Marca::xp).reversed()
                .thenComparing(Marca::llegoEl)
                .thenComparing(Marca::userId))
            .toList();
    }
}
