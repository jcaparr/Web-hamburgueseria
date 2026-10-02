package com.hamburguesas.places;

import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.FranjaHoraria;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FranjaHorariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Le pide a Google el horario de cada local y lo guarda.
 *
 * Se guarda para no pedirlo cada vez que alguien abre un local: eso sería una llamada
 * del tramo Enterprise por visita, y con un poco de tráfico la cuota del mes se iría en
 * un día. Guardado, cuesta una llamada por local cada tanto, y si el local está abierto
 * ahora se calcula en el navegador.
 *
 * El tope del mes no alcanza para todos los locales, y no hace falta: se pregunta
 * primero por los que nunca se preguntaron, así que lo que no entra en un mes lo
 * completa el siguiente.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HorariosDeLocales {

    /**
     * Cada cuánto se vuelve a preguntar por un local que ya tiene horario.
     *
     * Los horarios cambian poco, y sin este mínimo, una vez completos todos, cada pasada
     * volvería a preguntar por los que se preguntaron ayer.
     */
    static final Duration VIGENCIA = Duration.ofDays(30);

    /** Igual que en el barrido: diez errores seguidos es que Google no está. */
    private static final int FALLAS_PARA_RENDIRSE = 10;

    private final LlamadasAGoogle google;
    private final BurgerJointRepository burgerJointRepository;
    private final FranjaHorariaRepository franjaRepository;

    public HorariosPedidos completar() {
        List<BurgerJoint> pendientes =
            burgerJointRepository.paraPedirleElHorario(Instant.now().minus(VIGENCIA));

        int conHorario = 0;
        int sinHorario = 0;
        int fallidos = 0;
        int fallasSeguidas = 0;
        int preguntados = 0;
        String aviso = null;

        for (BurgerJoint local : pendientes) {
            if (!google.quedan(PlacesCallType.HORARIO)) {
                aviso = "Se terminó la cuota de horarios del mes ("
                    + google.limiteDe(PlacesCallType.HORARIO) + ")";
                break;
            }

            List<HorarioDeGoogle.Franja> franjas;
            try {
                franjas = google.horarioDe(local.getPlaceId());
                fallasSeguidas = 0;
            } catch (RestClientException ex) {
                // No se marca como preguntado: no saber no es lo mismo que "no tiene
                // horario", y sin la fecha vuelve a salir primero en la próxima pasada.
                log.warn("No se pudo pedir el horario de {}: {}", local.getPlaceId(), ex.getMessage());
                fallidos++;
                preguntados++;
                if (++fallasSeguidas >= FALLAS_PARA_RENDIRSE) {
                    aviso = "Google falló " + fallasSeguidas + " veces seguidas, se frenó";
                    break;
                }
                continue;
            }

            guardar(local, franjas);
            preguntados++;
            if (franjas.isEmpty()) {
                sinHorario++;
            } else {
                conHorario++;
            }
        }

        int faltan = pendientes.size() - preguntados;
        log.info("Horarios: {} guardados, {} sin horario en Google, {} fallidos, {} para otro mes",
            conHorario, sinHorario, fallidos, faltan);
        return new HorariosPedidos(conHorario, sinHorario, fallidos, faltan, aviso);
    }

    /** Reemplaza el horario entero: Google lo devuelve completo, no los cambios. */
    private void guardar(BurgerJoint local, List<HorarioDeGoogle.Franja> franjas) {
        franjaRepository.borrarLasDe(local.getId());
        franjaRepository.saveAll(franjas.stream()
            .map(franja -> FranjaHoraria.builder()
                .burgerJoint(local)
                .dia(franja.dia())
                .abre(franja.abre())
                .cierra(franja.cierra())
                .build())
            .toList());

        // Solo la fecha, y no el local entero: la pasada dura minutos y guardar lo que
        // se leyó al empezar pisaría lo que otro trabajo haya cambiado mientras tanto.
        burgerJointRepository.anotarHorarioConsultado(local.getId(), Instant.now());
    }
}
