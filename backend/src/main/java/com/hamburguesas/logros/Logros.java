package com.hamburguesas.logros;

import com.hamburguesas.logros.CifrasDeLogros.Cifras;
import com.hamburguesas.logros.CifrasDeLogros.Resenia;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.UserRepository;
import com.hamburguesas.service.Niveles;
import com.hamburguesas.texto.Texto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Qué logros tiene cada cuenta y cuáles le tocan hoy (#314).
 *
 * {@link #recalcular} deja los logros de una cuenta como corresponde a lo que hay en la
 * base ahora: inserta los nuevos, reactiva los que volvió a cumplir y desactiva los
 * propios que dejó de cumplir. Es idempotente y recalcula todo el catálogo para esa
 * persona, no una familia por vez: así no depende de acordarse de qué cambió.
 *
 * <p>Qué se pierde: los propios se desactivan si dejan de cumplirse; los ajenos (los que
 * dan otros) quedan una vez ganados. Perder y volver a ganar no repite el aviso, porque
 * {@code vistoEl} no se toca.
 */
@Service
@RequiredArgsConstructor
public class Logros {

    /** El día y la hora de las reglas (Bajonero, Patriota) son los de Buenos Aires. */
    public static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    /** Los que no se miran acá porque los entrega el cierre del podio (#318). */
    private static final Set<Logro> DEL_PODIO =
        EnumSet.of(Logro.PODIO_SEMANAL, Logro.PRIMERO_DEL_MES, Logro.RACHA_DE_ORO);

    /** Las marcas de Paladar de plástico, como queda el principio del nombre sin signos ni acentos. */
    private static final List<String> MARCAS_GRANDES = List.of("burgerking", "mcdonalds", "wendys");

    private final LogroDeUsuarioRepository repository;
    private final UserRepository userRepository;
    private final CifrasDeLogros cifras;

    /**
     * @param retroactivo true en la carga inicial: lo que ya merecían las cuentas que
     *   existían al lanzar los logros. Se avisa con un solo mensaje, sin animación.
     * @return los logros que quedaron activos
     */
    // En una transacción propia siempre: el recálculo corre después de confirmar el cambio que lo
    // pidió (no se puede escribir en esa transacción) y cada cuenta tiene la suya, así una que falla
    // no arrastra a las demás ni se toman dos locks de cuentas en la misma transacción.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Set<Logro> recalcular(Long userId, boolean retroactivo) {
        // La fila de la cuenta se bloquea: dos recálculos de la misma cuenta se esperan.
        User cuenta = userRepository.bloquear(userId).orElse(null);
        if (cuenta == null) {
            return Set.of();
        }
        Cifras c = cifras.de(cuenta);
        Map<Logro, LogroDeUsuario> actuales = new HashMap<>();
        for (LogroDeUsuario fila : repository.deLaCuenta(userId)) {
            actuales.put(fila.logro(), fila);
        }

        Set<Logro> cumple = EnumSet.noneOf(Logro.class);
        for (Logro logro : Logro.values()) {
            if (logro != Logro.MENU_COMPLETO && !DEL_PODIO.contains(logro) && cumple(logro, c)) {
                cumple.add(logro);
            }
        }

        // Los ajenos (y los del podio) que ya se ganaron siguen ahí aunque lo que los dio haya cambiado.
        Set<Logro> activosDespues = EnumSet.noneOf(Logro.class);
        for (Logro logro : Logro.values()) {
            LogroDeUsuario fila = actuales.get(logro);
            boolean sigueActivo = fila != null && fila.isActivo() && (logro.ajeno() || DEL_PODIO.contains(logro));
            if (cumple.contains(logro) || sigueActivo) {
                activosDespues.add(logro);
            }
        }
        // Menú completo: todos los demás, secretos incluidos.
        if (activosDespues.containsAll(EnumSet.complementOf(EnumSet.of(Logro.MENU_COMPLETO)))) {
            activosDespues.add(Logro.MENU_COMPLETO);
        }

        Instant ahora = Instant.now();
        for (Logro logro : Logro.values()) {
            if (DEL_PODIO.contains(logro)) {
                continue;
            }
            LogroDeUsuario fila = actuales.get(logro);
            boolean activo = activosDespues.contains(logro);
            String detalle = logro == Logro.INTENDENTE ? c.barrioDondeEsIntendente() : null;
            if (fila == null) {
                if (activo) {
                    repository.save(new LogroDeUsuario(new LogroDeUsuarioId(userId, logro.name()),
                        ahora, true, ahora, null, retroactivo, detalle));
                }
                continue;
            }
            if (fila.isActivo() != activo) {
                fila.setActivo(activo);
                fila.setCambioEl(ahora);
            }
            if (activo && detalle != null && !detalle.equals(fila.getDetalle())) {
                fila.setDetalle(detalle);
            }
        }
        return activosDespues;
    }

    /**
     * La pantalla de Logros (#316): recalcula antes de responder, así lo que se ve es lo que
     * hay hoy y no lo que quedó de la última vez que algo avisó.
     */
    @Transactional
    public PantallaDeLogros pantalla(Long userId) {
        Set<Logro> activos = recalcular(userId, false);
        User cuenta = userRepository.findById(userId).orElseThrow();
        Cifras c = cifras.de(cuenta);
        int xp = Niveles.xpTotal(c.resenias().stream().map(Resenia::comentario).toList());
        int nivel = Niveles.nivelDe(xp);

        Map<Logro, LogroDeUsuario> filas = new HashMap<>();
        for (LogroDeUsuario fila : repository.deLaCuenta(userId)) {
            filas.put(fila.logro(), fila);
        }
        List<PantallaDeLogros.Item> items = java.util.Arrays.stream(Logro.values()).map(logro -> {
            boolean ganado = activos.contains(logro);
            LogroDeUsuario fila = filas.get(logro);
            // De un secreto que todavía no se ganó no se dice nada: ni cuánto falta.
            int[] avance = ganado || !logro.secreto() ? avance(logro, c, nivel, activos) : null;
            return new PantallaDeLogros.Item(logro.name(), ganado, ganado && fila != null ? fila.getGanadoEl() : null,
                avance == null ? null : avance[0], avance == null ? null : avance[1],
                ganado && fila != null ? fila.getDetalle() : null);
        }).toList();

        return new PantallaDeLogros(nivel, xp, Niveles.UMBRALES[nivel - 1],
            nivel < Niveles.NIVEL_MAXIMO ? Niveles.UMBRALES[nivel] : null, items);
    }

    /** {actual, meta} en los logros que se miden con un número; null en los que se tienen o no. */
    static int[] avance(Logro logro, Cifras c, int nivel, Set<Logro> activos) {
        List<Resenia> propias = c.resenias();
        Map<String, Long> porBarrio = propias.stream()
            .filter(r -> !r.cadena() && r.area() != null && !r.area().isBlank())
            .collect(Collectors.groupingBy(Resenia::area, Collectors.counting()));
        long locales = propias.stream().filter(r -> !r.cadena()).map(Resenia::localId).distinct().count();
        return switch (logro) {
            case NIVEL_3 -> par(nivel, 3);
            case NIVEL_5 -> par(nivel, 5);
            case NIVEL_10 -> par(nivel, 10);
            case NIVEL_15 -> par(nivel, 15);
            case NIVEL_20 -> par(nivel, 20);
            case SEGUIDORES_10 -> par(c.seguidores(), 10);
            case SEGUIDORES_50 -> par(c.seguidores(), 50);
            case SEGUIDORES_100 -> par(c.seguidores(), 100);
            case SEGUIDORES_500 -> par(c.seguidores(), 500);
            case SEGUIDORES_1000 -> par(c.seguidores(), 1000);
            case BARRIO_ESQUINA -> par(porBarrio.values().stream().mapToLong(Long::longValue).max().orElse(0), 3);
            case BARRIOS_5 -> par(porBarrio.size(), 5);
            case BARRIOS_15 -> par(porBarrio.size(), 15);
            case BARRIOS_25 -> par(porBarrio.size(), 25);
            case HAMBRE_AJENA -> par(c.masReaccionesEnUnaResenia(), 10);
            case PLANIFICADOR -> par(c.guardadas(), 10);
            case STALKER -> par(c.reaccionesQueDio(), 30);
            case GUIA -> par(c.recorridos(), 5);
            case PROMESA -> par(c.reseniasConGuardadaPrevia(), 5);
            case DE_AUTOR -> par(locales, 10);
            case COMEDIANTE -> par(c.risasRecibidas(), 10);
            case BOCA_EN_BOCA -> par(c.seguidoresQueReseniaronDespues(), 3);
            case MENU_COMPLETO -> par(activos.stream().filter(l -> l != Logro.MENU_COMPLETO).count(), Logro.values().length - 1);
            default -> null;
        };
    }

    /** El avance nunca pasa de la meta: la barra se llena y no se desborda. */
    private static int[] par(long actual, int meta) {
        return new int[] {(int) Math.min(actual, meta), meta};
    }

    /** La regla de cada logro contra las cifras de la cuenta. */
    static boolean cumple(Logro logro, Cifras c) {
        int nivel = Niveles.nivelDe(Niveles.xpTotal(
            c.resenias().stream().map(Resenia::comentario).toList()));
        List<Resenia> propias = c.resenias();
        Map<String, Long> porBarrio = propias.stream()
            .filter(r -> !r.cadena() && r.area() != null && !r.area().isBlank())
            .collect(Collectors.groupingBy(Resenia::area, Collectors.counting()));

        return switch (logro) {
            case NIVEL_1 -> true;
            case NIVEL_3 -> nivel >= 3;
            case NIVEL_5 -> nivel >= 5;
            case NIVEL_10 -> nivel >= 10;
            case NIVEL_15 -> nivel >= 15;
            case NIVEL_20 -> nivel >= 20;
            case SEGUIDORES_10 -> c.seguidores() >= 10;
            case SEGUIDORES_50 -> c.seguidores() >= 50;
            case SEGUIDORES_100 -> c.seguidores() >= 100;
            case SEGUIDORES_500 -> c.seguidores() >= 500;
            case SEGUIDORES_1000 -> c.seguidores() >= 1000;
            case BARRIO_ESQUINA -> porBarrio.values().stream().anyMatch(n -> n >= 3);
            case BARRIOS_5 -> porBarrio.size() >= 5;
            case BARRIOS_15 -> porBarrio.size() >= 15;
            case BARRIOS_25 -> porBarrio.size() >= 25;
            case INTENDENTE -> c.barrioDondeEsIntendente() != null;
            case PRIMERA_RESENIA -> !propias.isEmpty();
            case PICASSO -> c.armoSuHamburguesa();
            case ANSIOSO -> propias.stream().anyMatch(Resenia::primeraConComentario);
            case HAMBRE_AJENA -> c.masReaccionesEnUnaResenia() >= 10;
            case PLANIFICADOR -> c.guardadas() >= 10;
            case STALKER -> c.reaccionesQueDio() >= 30;
            case PRIMER_TOUR -> c.recorridos() >= 1;
            case GUIA -> c.recorridos() >= 5;
            case ALBUM -> propias.stream().anyMatch(r -> r.fotos() >= 3);
            case PROMESA -> c.reseniasConGuardadaPrevia() >= 5;
            case DE_AUTOR -> propias.stream().filter(r -> !r.cadena()).map(Resenia::localId).distinct().count() >= 10;
            case PIONERO -> c.esDeLasPrimeras();
            case COMBO -> c.unaConLosCincoTipos();
            case BOCA_EN_BOCA -> c.seguidoresQueReseniaronDespues() >= 3;
            case COMEDIANTE -> c.risasRecibidas() >= 10;
            case BAJONERO -> propias.stream().anyMatch(r -> {
                int hora = ZonedDateTime.ofInstant(r.creada(), ZONA).getHour();
                return hora >= 1 && hora < 7;
            });
            case PATRIOTA -> propias.stream().anyMatch(r -> {
                ZonedDateTime fecha = ZonedDateTime.ofInstant(r.creada(), ZONA);
                return fecha.getMonthValue() == 5 && fecha.getDayOfMonth() == 28;
            });
            case PALADAR -> MARCAS_GRANDES.stream().allMatch(marca -> propias.stream()
                .anyMatch(r -> Texto.soloLetrasYNumeros(r.nombreDelLocal()).startsWith(marca)));
            case MENU_COMPLETO, PODIO_SEMANAL, PRIMERO_DEL_MES, RACHA_DE_ORO -> false;
        };
    }
}
