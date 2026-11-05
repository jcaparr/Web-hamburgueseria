package com.hamburguesas.service;

import com.hamburguesas.dto.AvisoDeTuMesDto;
import com.hamburguesas.dto.ReseniaDelMesDto;
import com.hamburguesas.dto.TuMesDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.podio.ResultadoDePodioRepository;
import com.hamburguesas.model.TipoDeReaccion;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.ReaccionRepository;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Tu mes en Burgómetro" (#308): el resumen de un mes de reseñas de una persona.
 *
 * No se guarda nada: se arma al pedirlo con las reseñas de esa persona en el período, que
 * son pocas y salen filtradas por usuario y por fecha. Un resumen se puede pedir recién
 * cuando el mes terminó, así que lo que cuenta ya no cambia salvo que borre una reseña.
 * Las fechas son de Buenos Aires y los períodos van de la medianoche de ahí, [desde, hasta):
 * una reseña del domingo 23:30 es del mes que cierra y una del lunes 00:30, del siguiente.
 */
@Service

public class TuMes {

    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");
    /** Antes de esto no hay reseñas: un año absurdo en la dirección daría un error de fecha, y acá un 404. */
    static final int PRIMER_ANIO = 2020;

    private final RatingRepository ratingRepository;
    private final LogroDeUsuarioRepository logroRepository;
    private final ResultadoDePodioRepository podioRepository;
    private final ReaccionRepository reaccionRepository;
    private final Bloqueos bloqueos;
    private final UserRepository userRepository;
    private final Clock reloj;

    @Autowired
    public TuMes(RatingRepository ratingRepository, LogroDeUsuarioRepository logroRepository, ResultadoDePodioRepository podioRepository, ReaccionRepository reaccionRepository, Bloqueos bloqueos, UserRepository userRepository) {
        this(ratingRepository, logroRepository, podioRepository, reaccionRepository, bloqueos, userRepository, Clock.systemUTC());
    }

    /** Con otro reloj, para los tests. */
    TuMes(RatingRepository ratingRepository, LogroDeUsuarioRepository logroRepository, ResultadoDePodioRepository podioRepository, ReaccionRepository reaccionRepository, Bloqueos bloqueos, UserRepository userRepository, Clock reloj) {
        this.ratingRepository = ratingRepository;
        this.logroRepository = logroRepository;
        this.podioRepository = podioRepository;
        this.reaccionRepository = reaccionRepository;
        this.bloqueos = bloqueos;
        this.userRepository = userRepository;
        this.reloj = reloj;
    }

    /** El mes que se resume hoy: el último que terminó, nunca el que está corriendo. */
    public static YearMonth mesQueSeResume(LocalDate hoy) {
        return YearMonth.from(hoy).minusMonths(1);
    }

    /** El resumen del último mes terminado, o el de {@code mes} si se pide uno. Con cero reseñas, resenias = 0. */
    @Transactional(readOnly = true)
    public TuMesDto resumen(Long userId, YearMonth mes) {
        YearMonth ultimo = mesQueSeResume(LocalDate.now(reloj.withZone(ZONA)));
        YearMonth elegido = mes == null ? ultimo : mes;
        // Un mes que todavía corre cambia con cada reseña: no es un resumen.
        if (elegido.isAfter(ultimo) || elegido.getYear() < PRIMER_ANIO) {
            throw new ResourceNotFoundException("Ese mes todavía no terminó");
        }
        User usuario = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return armar(usuario, elegido);
    }

    /** Un período a resumir: un mes, o el año entero en el anual. */
    record Periodo(YearMonth mes, boolean anual, LocalDate desde, LocalDate hasta) {
        static Periodo de(YearMonth mes) {
            if (mes.getMonth() == Month.DECEMBER) {
                return new Periodo(mes, true, LocalDate.of(mes.getYear(), 1, 1), LocalDate.of(mes.getYear() + 1, 1, 1));
            }
            return new Periodo(mes, false, mes.atDay(1), mes.plusMonths(1).atDay(1));
        }

        Instant instanteDesde() {
            return desde.atStartOfDay(ZONA).toInstant();
        }

        Instant instanteHasta() {
            return hasta.atStartOfDay(ZONA).toInstant();
        }

        /** El período de antes, del mismo largo: el mes anterior o el año anterior. */
        Periodo anterior() {
            LocalDate d = anual ? desde.minusYears(1) : desde.minusMonths(1);
            return new Periodo(mes, anual, d, desde);
        }
    }

    /**
     * Si hay un resumen para avisar hoy (#405): el del último mes terminado, que sale a las
     * 00:00 de Buenos Aires del primer día del mes siguiente (el de noviembre, el 1 de
     * diciembre; el anual, el 1 de enero). Solo cuenta si hubo al menos una reseña en el
     * período. Se arma al preguntar, sin tabla ni tarea: es correcto cualquier día y hora, y
     * pedirlo de nuevo da lo mismo.
     */
    @Transactional(readOnly = true)
    public AvisoDeTuMesDto aviso(Long userId) {
        LocalDate hoy = LocalDate.now(reloj.withZone(ZONA));
        YearMonth ultimo = mesQueSeResume(hoy);
        Instant desde = hoy.withDayOfMonth(1).atStartOfDay(ZONA).toInstant();
        Periodo p = Periodo.de(ultimo);
        Object[] fila = ratingRepository.cuantasEntre(userId, p.instanteDesde(), p.instanteHasta()).get(0);
        if (((Number) fila[0]).intValue() == 0) {
            return new AvisoDeTuMesDto(false, ultimo.toString(), p.anual(), false, desde);
        }
        User usuario = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        boolean visto = usuario.getResumenVisto() != null && usuario.getResumenVisto().compareTo(ultimo.toString()) >= 0;
        return new AvisoDeTuMesDto(true, ultimo.toString(), p.anual(), visto, desde);
    }

    /** Anota que la persona ya vio el resumen de hoy, para no mostrarle el pop up de nuevo. Nunca retrocede. */
    @Transactional
    public void marcarVisto(Long userId) {
        String ultimo = mesQueSeResume(LocalDate.now(reloj.withZone(ZONA))).toString();
        User usuario = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        if (usuario.getResumenVisto() == null || usuario.getResumenVisto().compareTo(ultimo) < 0) {
            usuario.setResumenVisto(ultimo);
            userRepository.save(usuario);
        }
    }

    TuMesDto armar(User usuario, YearMonth mes) {
        Periodo p = Periodo.de(mes);
        Instant desde = p.instanteDesde();
        Instant hasta = p.instanteHasta();
        List<ReseniaDelMesDto> resenias = ratingRepository.deLaPersonaEntre(usuario.getId(), desde, hasta);
        String clave = mes.toString();
        int nivelDeHoy = Niveles.nivelDe(usuario.getXp());
        if (resenias.isEmpty()) {
            return new TuMesDto(clave, p.anual(), 0, 0, List.of(), 0, null, nivelDeHoy, usuario.getXp(), 0,
                List.of(), List.of(), null, null, null);
        }

        Map<String, Integer> porBarrio = new HashMap<>();
        for (ReseniaDelMesDto r : resenias) {
            if (r.barrio() != null && !r.barrio().isBlank()) {
                porBarrio.merge(r.barrio(), 1, Integer::sum);
            }
        }
        List<TuMesDto.Barrio> barrios = porBarrio.entrySet().stream()
            .map(e -> new TuMesDto.Barrio(e.getKey(), e.getValue()))
            .sorted(Comparator.comparingInt(TuMesDto.Barrio::resenias).reversed()
                .thenComparing(TuMesDto.Barrio::nombre))
            .toList();

        // La mejor nota; en el empate, la que dio primero, y por último el id para que no cambie.
        ReseniaDelMesDto mejor = resenias.stream()
            .min(Comparator.comparingInt(ReseniaDelMesDto::puntaje).reversed()
                .thenComparing(ReseniaDelMesDto::cuando).thenComparing(ReseniaDelMesDto::id))
            .orElseThrow();

        var ganados = logroRepository.ganadosEnElPeriodo(usuario.getId(), desde, hasta).stream()
            .map(l -> new TuMesDto.LogroGanado(l.getId().logro(), l.getDetalle()))
            .toList();
        // Los podios que empezaron en el período: solo los puestos de esta persona, sin nadie más.
        var podios = podioRepository.deLaPersonaEntre(usuario.getId(), p.desde(), p.hasta()).stream()
            .map(r -> new TuMesDto.Puesto(r.getId().periodo(), r.getId().desde(), r.getId().puesto()))
            .toList();

        Periodo antes = p.anterior();
        int reseniasAnteriores = ((Number) ratingRepository
            .cuantasEntre(usuario.getId(), antes.instanteDesde(), antes.instanteHasta()).get(0)[0]).intValue();

        return new TuMesDto(clave, p.anual(), resenias.size(), reseniasAnteriores,
            barrios, barrios.size(),
            new TuMesDto.Mejor(mejor.localId(), mejor.slug(), mejor.nombre(), mejor.barrio(), mejor.puntaje()),
            nivelDeHoy, usuario.getXp(), ganados.size(), ganados, podios,
            diaFavorito(resenias), reacciones(usuario.getId(), desde, hasta),
            p.anual() ? recuento(usuario.getId(), resenias, desde, hasta) : null);
    }

    static final List<String> FRANJAS = List.of("madrugada", "mañana", "tarde", "noche");
    static final int MINIMO_PARA_EL_DIA = 3;

    /**
     * El día de la semana en que más reseñó y la franja en que más lo hizo ese día, en hora
     * de Buenos Aires. Con menos de tres reseñas no hay "favorito" que decir; en el empate
     * gana el día más temprano de la semana y, entre franjas, la más temprana.
     */
    static TuMesDto.Dia diaFavorito(List<ReseniaDelMesDto> resenias) {
        if (resenias.size() < MINIMO_PARA_EL_DIA) {
            return null;
        }
        Map<DayOfWeek, List<ReseniaDelMesDto>> porDia = new EnumMap<>(DayOfWeek.class);
        for (ReseniaDelMesDto r : resenias) {
            porDia.computeIfAbsent(r.cuando().atZone(ZONA).getDayOfWeek(), d -> new ArrayList<>()).add(r);
        }
        DayOfWeek mejorDia = null;
        for (DayOfWeek d : DayOfWeek.values()) {
            if (porDia.containsKey(d) && (mejorDia == null || porDia.get(d).size() > porDia.get(mejorDia).size())) {
                mejorDia = d;
            }
        }
        int[] porFranja = new int[FRANJAS.size()];
        for (ReseniaDelMesDto r : porDia.get(mejorDia)) {
            porFranja[FRANJAS.indexOf(franjaDe(r.cuando().atZone(ZONA).getHour()))]++;
        }
        int mejorFranja = 0;
        for (int i = 1; i < porFranja.length; i++) {
            if (porFranja[i] > porFranja[mejorFranja]) {
                mejorFranja = i;
            }
        }
        return new TuMesDto.Dia(mejorDia.getValue(), FRANJAS.get(mejorFranja), porDia.get(mejorDia).size());
    }

    static String franjaDe(int hora) {
        if (hora < 6) {
            return "madrugada";
        }
        if (hora < 12) {
            return "mañana";
        }
        return hora < 20 ? "tarde" : "noche";
    }

    /** Las reacciones que recibieron sus reseñas, sin las de quien tiene un bloqueo de por medio. */
    private TuMesDto.Reacciones reacciones(Long userId, Instant desde, Instant hasta) {
        int total = 0;
        TipoDeReaccion masUsada = null;
        long mejorCuenta = 0;
        for (Object[] fila : reaccionRepository.recibidasEntre(userId, bloqueos.queNoPuedeVer(userId), desde, hasta)) {
            long cuantas = ((Number) fila[1]).longValue();
            TipoDeReaccion tipo = (TipoDeReaccion) fila[0];
            total += (int) cuantas;
            // En el empate, la primera del catálogo, para que no cambie entre pedidos.
            if (cuantas > mejorCuenta || (cuantas == mejorCuenta && tipo.ordinal() < masUsada.ordinal())) {
                mejorCuenta = cuantas;
                masUsada = tipo;
            }
        }
        return total == 0 ? null : new TuMesDto.Reacciones(total, masUsada.name());
    }

    /** Lo propio del anual: el mes con más reseñas y el nivel con el que empezó y terminó el año. */
    private TuMesDto.Recuento recuento(Long userId, List<ReseniaDelMesDto> resenias, Instant desde, Instant hasta) {
        int[] porMes = new int[13];
        for (ReseniaDelMesDto r : resenias) {
            porMes[r.cuando().atZone(ZONA).getMonthValue()]++;
        }
        int mejorMes = 1;
        for (int m = 2; m <= 12; m++) {
            if (porMes[m] > porMes[mejorMes]) {
                mejorMes = m;
            }
        }
        // La XP que tenía en cada fecha sale de sus reseñas hasta ese momento: la cuenta no guarda historia.
        int nivelAlEmpezar = Niveles.nivelDe(Niveles.xpTotal(ratingRepository.comentariosAntesDe(userId, desde)));
        int nivelAlTerminar = Niveles.nivelDe(Niveles.xpTotal(ratingRepository.comentariosAntesDe(userId, hasta)));
        return new TuMesDto.Recuento(mejorMes, porMes[mejorMes], nivelAlEmpezar, nivelAlTerminar);
    }
}
