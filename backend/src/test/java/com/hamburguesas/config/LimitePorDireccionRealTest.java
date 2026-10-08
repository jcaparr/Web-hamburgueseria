package com.hamburguesas.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El tope de la API con la cadena de filtros de verdad, como está en producción (#201).
 *
 * Lo que se prueba es el orden, que los tests del filtro solo no pueden ver. En
 * producción todos los pedidos llegan desde Caddy, y la dirección de quien pide viene
 * en X-Forwarded-For. Si el tope corriera antes de leerla, toda la web compartiría un
 * solo cupo de trescientos por minuto. Y si corriera después de la seguridad, lo que
 * ella rechaza no se contaría.
 *
 * Levanta el servidor en un puerto y le pide por HTTP. Cada test usa sus propias
 * direcciones, porque el cupo dura mientras dure el contexto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.forward-headers-strategy=framework")
@ActiveProfiles("test")
class LimitePorDireccionRealTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @Autowired private Environment entorno;

    private int pedir(String camino, String direccion) throws Exception {
        HttpRequest pedido = HttpRequest.newBuilder(
                URI.create("http://localhost:" + entorno.getProperty("local.server.port") + camino))
            .header("X-Forwarded-For", direccion)
            .GET().build();
        return HTTP.send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    /**
     * Pide hasta que lo frenen, y dice si lo frenaron.
     *
     * No alcanza con pedir el cupo justo y esperar el 429 en el siguiente: el cupo se
     * recarga a cinco por segundo mientras tanto. Con doscientos de margen hay tiempo
     * de sobra para que el freno aparezca, y si no aparece es que no está.
     */
    private boolean loFrenan(String camino, String direccion) throws Exception {
        for (int i = 0; i < ApiRateLimitFilter.PEDIDOS_POR_MINUTO + 200; i++) {
            if (pedir(camino, direccion) == 429) {
                return true;
            }
        }
        return false;
    }

    @Test
    void elCupoEsDeLaDireccionRealYNoDeCaddy() throws Exception {
        assertThat(loFrenan("/api/burger-joints/barrios", "181.0.0.1")).isTrue();

        // Enseguida, otra dirección. Si el cupo fuera uno solo para todos, estaría vacío
        // y diez pedidos seguidos no entrarían: se recarga uno cada doscientos ms.
        for (int i = 0; i < 10; i++) {
            assertThat(pedir("/api/burger-joints/barrios", "190.0.0.2")).isEqualTo(200);
        }
    }

    /** El feed pide sesión: sin ella, la seguridad lo rechaza, pero igual cuenta. */
    @Test
    void loQueRechazaLaSeguridadTambienCuenta() throws Exception {
        assertThat(pedir("/api/feed", "181.0.0.3")).isEqualTo(401);

        assertThat(loFrenan("/api/feed", "181.0.0.3")).isTrue();
    }
}
