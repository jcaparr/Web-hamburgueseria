package com.hamburguesas.places;

import com.hamburguesas.dto.HorarioDto;
import com.hamburguesas.model.BurgerJoint;
import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.places.HorarioDeGoogle.Franja;
import com.hamburguesas.repository.BurgerJointRepository;
import com.hamburguesas.repository.FranjaHorariaRepository;
import com.hamburguesas.service.BurgerJointService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La pasada que le pide el horario a Google y lo guarda.
 *
 * Contra una base de verdad, porque lo que más importa es a quién se le pregunta
 * primero: la cuota del mes no alcanza para todos, y tiene que irse a los que nunca se
 * preguntaron antes que a refrescar a los que ya tienen. Google, en cambio, es de
 * mentira: ningún test gasta cuota.
 */
@SpringBootTest
@ActiveProfiles("test")
class HorariosDeLocalesTest {

    @Autowired private BurgerJointRepository repository;
    @Autowired private FranjaHorariaRepository franjas;
    @Autowired private BurgerJointService service;

    private PlacesClient placesClient;
    private PlacesQuotaGuard quotaGuard;
    private HorariosDeLocales horarios;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        PlacesProperties properties = new PlacesProperties();
        properties.getSync().setDelayBetweenCallsMs(0);
        placesClient = mock(PlacesClient.class);
        quotaGuard = mock(PlacesQuotaGuard.class);
        when(quotaGuard.canCall(PlacesCallType.HORARIO)).thenReturn(true);

        horarios = new HorariosDeLocales(
            new LlamadasAGoogle(placesClient, quotaGuard, properties), repository, franjas);
    }

    private BurgerJoint guardar(String placeId, Instant consultado) {
        return repository.save(BurgerJoint.builder()
            .name("Local " + placeId).address("Una dirección").area("Palermo")
            .placeId(placeId).horarioConsultadoEl(consultado)
            .build());
    }

    @Test
    void guardaLasFranjasYAnotaLaConsultaYElGasto() {
        BurgerJoint local = guardar("ChIJ-a", null);
        when(placesClient.horarioDe("ChIJ-a")).thenReturn(List.of(
            new Franja(5, 1140, 1500), new Franja(6, 1140, 1500)));

        HorariosPedidos pedidos = horarios.completar();

        assertThat(pedidos.conHorario()).isEqualTo(1);
        HorarioDto horario = service.horario(local.getId());
        assertThat(horario.consultado()).isTrue();
        assertThat(horario.franjas()).containsExactly(
            new HorarioDto.Franja(5, 1140, 1500), new HorarioDto.Franja(6, 1140, 1500));
        verify(quotaGuard).record(PlacesCallType.HORARIO);
    }

    @Test
    void volverAPreguntarReemplazaElHorarioEnVezDeSumarle() {
        BurgerJoint local = guardar("ChIJ-a", null);
        when(placesClient.horarioDe("ChIJ-a")).thenReturn(List.of(new Franja(1, 720, 900)));
        horarios.completar();

        // Pasado el mes vuelve a salir, y el local cambió de horario.
        repository.anotarHorarioConsultado(local.getId(), Instant.now().minus(40, ChronoUnit.DAYS));
        when(placesClient.horarioDe("ChIJ-a")).thenReturn(List.of(new Franja(2, 1200, 1380)));
        horarios.completar();

        assertThat(service.horario(local.getId()).franjas())
            .containsExactly(new HorarioDto.Franja(2, 1200, 1380));
    }

    @Test
    void sinHorarioEnGoogleQuedaConsultadoYNoSeVuelveAPreguntarEnElMes() {
        BurgerJoint local = guardar("ChIJ-a", null);
        when(placesClient.horarioDe("ChIJ-a")).thenReturn(List.of());

        assertThat(horarios.completar().sinHorario()).isEqualTo(1);
        assertThat(horarios.completar().sinHorario()).isZero();

        HorarioDto horario = service.horario(local.getId());
        assertThat(horario.consultado()).isTrue();
        assertThat(horario.franjas()).isEmpty();
    }

    @Test
    void siGoogleFallaNoQuedaComoConsultadoYSeReintentaLaProxima() {
        BurgerJoint local = guardar("ChIJ-a", null);
        when(placesClient.horarioDe("ChIJ-a"))
            .thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

        HorariosPedidos pedidos = horarios.completar();

        assertThat(pedidos.fallidos()).isEqualTo(1);
        assertThat(service.horario(local.getId()).consultado()).isFalse();
        verify(quotaGuard, never()).record(PlacesCallType.HORARIO);
    }

    @Test
    void primeroLosQueNuncaSePreguntaronYDespuesLosMasViejos() {
        guardar("ChIJ-viejo", Instant.now().minus(90, ChronoUnit.DAYS));
        guardar("ChIJ-nuevo", null);
        guardar("ChIJ-medio", Instant.now().minus(45, ChronoUnit.DAYS));
        when(placesClient.horarioDe(anyString())).thenReturn(List.of());

        // Cuota para uno solo: tiene que irse al que nunca se preguntó.
        when(quotaGuard.canCall(PlacesCallType.HORARIO)).thenReturn(true, false);
        HorariosPedidos pedidos = horarios.completar();

        verify(placesClient).horarioDe("ChIJ-nuevo");
        verify(placesClient, never()).horarioDe("ChIJ-viejo");
        assertThat(pedidos.faltan()).isEqualTo(2);
        assertThat(pedidos.aviso()).contains("cuota");

        assertThat(repository.paraPedirleElHorario(Instant.now().minus(HorariosDeLocales.VIGENCIA)))
            .extracting(BurgerJoint::getPlaceId)
            .containsExactly("ChIJ-viejo", "ChIJ-medio");
    }

    @Test
    void noSePreguntaPorLosQueTienenElHorarioFrescoNiPorLosCargadosAMano() {
        guardar("ChIJ-fresco", Instant.now().minus(3, ChronoUnit.DAYS));
        guardar(null, null);

        HorariosPedidos pedidos = horarios.completar();

        verify(placesClient, never()).horarioDe(anyString());
        assertThat(pedidos.faltan()).isZero();
    }

    @Test
    void borrarElLocalSeLlevaSusFranjas() {
        BurgerJoint local = guardar("ChIJ-a", null);
        when(placesClient.horarioDe("ChIJ-a")).thenReturn(List.of(new Franja(1, 720, 900)));
        horarios.completar();

        // La limpieza borra locales con delete(), sin mirar qué cuelga de ellos.
        repository.delete(local);

        assertThat(franjas.count()).isZero();
    }
}
