package com.hamburguesas.service;

import com.hamburguesas.dto.ReseniaDelMesDto;
import com.hamburguesas.dto.TuMesDto;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.logros.LogroDeUsuarioRepository;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.RatingRepository;
import com.hamburguesas.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
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
    /** Cuántos barrios muestra la tarjeta; el resto va como "+N". */
    static final int BARRIOS_VISIBLES = 3;
    /** Antes de esto no hay reseñas: un año absurdo en la dirección daría un error de fecha, y acá un 404. */
    static final int PRIMER_ANIO = 2020;

    private final RatingRepository ratingRepository;
    private final LogroDeUsuarioRepository logroRepository;
    private final UserRepository userRepository;
    private final Clock reloj;

    @Autowired
    public TuMes(RatingRepository ratingRepository, LogroDeUsuarioRepository logroRepository, UserRepository userRepository) {
        this(ratingRepository, logroRepository, userRepository, Clock.systemUTC());
    }

    /** Con otro reloj, para los tests. */
    TuMes(RatingRepository ratingRepository, LogroDeUsuarioRepository logroRepository, UserRepository userRepository, Clock reloj) {
        this.ratingRepository = ratingRepository;
        this.logroRepository = logroRepository;
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

    TuMesDto armar(User usuario, YearMonth mes) {
        Instant desde = mes.atDay(1).atStartOfDay(ZONA).toInstant();
        Instant hasta = mes.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant();
        List<ReseniaDelMesDto> resenias = ratingRepository.deLaPersonaEntre(usuario.getId(), desde, hasta);
        String clave = mes.toString();
        if (resenias.isEmpty()) {
            return new TuMesDto(clave, 0, List.of(), 0, null, Niveles.nivelDe(usuario.getXp()), usuario.getXp(), 0, null);
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

        int logros = (int) logroRepository.ganadosEntre(usuario.getId(), desde, hasta);
        return new TuMesDto(clave, resenias.size(),
            barrios.stream().limit(BARRIOS_VISIBLES).toList(), barrios.size(),
            new TuMesDto.Mejor(mejor.localId(), mejor.slug(), mejor.nombre(), mejor.barrio(), mejor.foto(), mejor.puntaje()),
            Niveles.nivelDe(usuario.getXp()), usuario.getXp(), logros,
            mes.getMonth() == Month.DECEMBER ? delAnio(usuario.getId(), mes.getYear()) : null);
    }

    /** Lo del año entero, para la tarjeta extra del resumen de diciembre. */
    private TuMesDto.Anio delAnio(Long userId, int anio) {
        Instant desde = LocalDate.of(anio, 1, 1).atStartOfDay(ZONA).toInstant();
        Instant hasta = LocalDate.of(anio + 1, 1, 1).atStartOfDay(ZONA).toInstant();
        Object[] fila = ratingRepository.cuantasEntre(userId, desde, hasta).get(0);
        return new TuMesDto.Anio(((Number) fila[0]).intValue(), ((Number) fila[1]).intValue());
    }
}
