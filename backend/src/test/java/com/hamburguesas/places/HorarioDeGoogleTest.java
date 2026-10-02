package com.hamburguesas.places;

import com.hamburguesas.places.HorarioDeGoogle.Franja;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cómo se lee el horario que devuelve Google.
 *
 * Los casos son los que se leen mal si se lee a la ligera: el que cierra pasada la
 * medianoche, el que abre siempre y el que Google no sabe hasta cuándo abre.
 */
class HorarioDeGoogleTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static JsonNode ficha(String periodos) {
        return MAPPER.readTree("{ \"regularOpeningHours\": { \"periods\": " + periodos + " } }");
    }

    private static String momento(int dia, int hora, int minuto) {
        return "{ \"day\": %d, \"hour\": %d, \"minute\": %d }".formatted(dia, hora, minuto);
    }

    @Test
    void unaFranjaDentroDelMismoDia() {
        JsonNode ficha = ficha("[{ \"open\": " + momento(2, 12, 0)
            + ", \"close\": " + momento(2, 15, 30) + " }]");

        assertThat(HorarioDeGoogle.franjas(ficha))
            .containsExactly(new Franja(2, 12 * 60, 15 * 60 + 30));
    }

    @Test
    void laQueCierraPasadaLaMedianocheQuedaEnElDiaQueAbre() {
        // Viernes de 19 a 1 de la mañana: se lee "el viernes, de 19 a 1", no un sábado de 0 a 1.
        JsonNode ficha = ficha("[{ \"open\": " + momento(5, 19, 0)
            + ", \"close\": " + momento(6, 1, 0) + " }]");

        assertThat(HorarioDeGoogle.franjas(ficha))
            .containsExactly(new Franja(5, 19 * 60, 25 * 60));
    }

    @Test
    void elSabadoQueCierraElDomingoTambien() {
        // Del 6 al 0 la resta da negativa: es la vuelta de la semana, no un error.
        JsonNode ficha = ficha("[{ \"open\": " + momento(6, 20, 0)
            + ", \"close\": " + momento(0, 2, 0) + " }]");

        assertThat(HorarioDeGoogle.franjas(ficha))
            .containsExactly(new Franja(6, 20 * 60, 26 * 60));
    }

    @Test
    void dosFranjasElMismoDiaSalenOrdenadas() {
        JsonNode ficha = ficha("["
            + "{ \"open\": " + momento(3, 20, 0) + ", \"close\": " + momento(3, 23, 0) + " },"
            + "{ \"open\": " + momento(1, 12, 0) + ", \"close\": " + momento(1, 15, 0) + " },"
            + "{ \"open\": " + momento(3, 12, 0) + ", \"close\": " + momento(3, 15, 0) + " }"
            + "]");

        assertThat(HorarioDeGoogle.franjas(ficha)).containsExactly(
            new Franja(1, 720, 900),
            new Franja(3, 720, 900),
            new Franja(3, 1200, 1380));
    }

    @Test
    void abiertoSiempreSonLosSieteDiasEnteros() {
        // Así lo dice Google: una apertura el domingo a medianoche y ningún cierre.
        JsonNode ficha = ficha("[{ \"open\": " + momento(0, 0, 0) + " }]");

        assertThat(HorarioDeGoogle.franjas(ficha)).containsExactly(
            new Franja(0, 0, 1440), new Franja(1, 0, 1440), new Franja(2, 0, 1440),
            new Franja(3, 0, 1440), new Franja(4, 0, 1440), new Franja(5, 0, 1440),
            new Franja(6, 0, 1440));
    }

    @Test
    void unaAperturaSinCierreQueNoEsAbiertoSiempreNoSeInventa() {
        JsonNode ficha = ficha("["
            + "{ \"open\": " + momento(2, 12, 0) + " },"
            + "{ \"open\": " + momento(3, 12, 0) + ", \"close\": " + momento(3, 15, 0) + " }"
            + "]");

        assertThat(HorarioDeGoogle.franjas(ficha)).containsExactly(new Franja(3, 720, 900));
    }

    @Test
    void sinHorarioEnGoogleNoHayFranjas() {
        assertThat(HorarioDeGoogle.franjas(MAPPER.readTree("{}"))).isEmpty();
        assertThat(HorarioDeGoogle.franjas(ficha("[]"))).isEmpty();
        assertThat(HorarioDeGoogle.franjas(null)).isEmpty();
    }
}
