package com.hamburguesas.places;

import com.hamburguesas.model.PlacesCallType;
import com.hamburguesas.places.PlacesSyncController.PlacesSyncStatus;
import com.hamburguesas.places.PlacesSyncController.UsoDeCuota;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lo que muestra /status, que es donde se mira cuánto queda de cada cuota de Google.
 *
 * El caso que lo motivó: en octubre la cuota de fichas llegó al tope (4.800 de 4.800) y
 * /status no la mostraba, porque solo tenía búsquedas y fotos.
 */
class EstadoDeCuotasTest {

    private final PlacesQuotaGuard cuotas = mock(PlacesQuotaGuard.class);
    private PlacesSyncController controlador;

    @BeforeEach
    void armar() {
        PlacesProperties propiedades = new PlacesProperties();
        propiedades.getSync().setTriggerToken("secreto");
        controlador = new PlacesSyncController(mock(PlacesSyncService.class), cuotas, propiedades);

        when(cuotas.mesEnCurso()).thenReturn("2026-10");
        when(cuotas.used(any())).thenReturn(10);
        when(cuotas.limitFor(any())).thenReturn(100);
    }

    @Test
    void muestraUnaCuotaPorCadaTipoDeLlamada() {
        PlacesSyncStatus estado = (PlacesSyncStatus) controlador.status("secreto").getBody();

        assertThat(estado.mes()).isEqualTo("2026-10");
        assertThat(estado.cuotas())
            .extracting(UsoDeCuota::tipo)
            .containsExactly(PlacesCallType.values());
    }

    @Test
    void unaCuotaAgotadaDiceQueNoQuedaNinguna() {
        when(cuotas.used(PlacesCallType.DETAILS)).thenReturn(4800);
        when(cuotas.limitFor(PlacesCallType.DETAILS)).thenReturn(4800);

        PlacesSyncStatus estado = (PlacesSyncStatus) controlador.status("secreto").getBody();

        assertThat(estado.cuotas())
            .filteredOn(cuota -> cuota.tipo() == PlacesCallType.DETAILS)
            .singleElement()
            .satisfies(fichas -> {
                assertThat(fichas.usadas()).isEqualTo(4800);
                assertThat(fichas.quedan()).isZero();
            });
    }

    @Test
    void sinElTokenNoMuestraNada() {
        ResponseEntity<?> respuesta = controlador.status("otro");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
