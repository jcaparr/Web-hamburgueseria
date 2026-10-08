package com.hamburguesas.config;

import com.hamburguesas.auth.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El tope de pedidos por dirección para toda la API (#201): frena a quien pide sin parar
 * y deja pasar las fotos, que una pantalla pide de a veinte.
 */
class LimiteDePedidosALaApiTest {

    /** Chico a propósito, para no tener que hacer trescientos pedidos en cada test. */
    private static final int CUPO = 3;

    private ApiRateLimitFilter filtro;

    @BeforeEach
    void setUp() {
        filtro = new ApiRateLimitFilter(new RateLimiter(), CUPO);
    }

    private MockHttpServletResponse pedir(String camino, String direccion) throws Exception {
        MockHttpServletRequest pedido = new MockHttpServletRequest("GET", camino);
        pedido.setRemoteAddr(direccion);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(pedido, respuesta, new MockFilterChain());
        return respuesta;
    }

    @Test
    void pasadoElCupoContesta429YDiceCuandoVolver() throws Exception {
        for (int i = 0; i < CUPO; i++) {
            assertThat(pedir("/api/burger-joints", "181.0.0.1").getStatus()).isEqualTo(200);
        }

        MockHttpServletResponse rechazada = pedir("/api/burger-joints", "181.0.0.1");

        assertThat(rechazada.getStatus()).isEqualTo(429);
        assertThat(rechazada.getHeader("Retry-After")).isEqualTo("60");
        assertThat(rechazada.getContentAsString()).contains("RATE_LIMITED");
    }

    /** El cupo es por dirección: que uno lo gaste no deja afuera a los demás. */
    @Test
    void cadaDireccionTieneSuCupo() throws Exception {
        for (int i = 0; i < CUPO + 5; i++) {
            pedir("/api/ranking/general", "181.0.0.1");
        }

        assertThat(pedir("/api/ranking/general", "190.0.0.2").getStatus()).isEqualTo(200);
    }

    /** Todos los caminos de la API cuentan para el mismo cupo, no uno por endpoint. */
    @Test
    void elCupoEsUnoParaTodaLaApi() throws Exception {
        pedir("/api/burger-joints", "181.0.0.1");
        pedir("/api/ranking/general", "181.0.0.1");
        pedir("/api/feed", "181.0.0.1");

        assertThat(pedir("/api/auth/me", "181.0.0.1").getStatus()).isEqualTo(429);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/place-photos/ChIJ123.jpg",
        "/api/rating-photos/66461845-df70-434e-b1dd-efba8c049946.jpg",
        "/sitemap.xml",
    })
    void lasFotosYLoQueNoEsLaApiNoCuentan(String camino) throws Exception {
        for (int i = 0; i < CUPO * 10; i++) {
            assertThat(pedir(camino, "181.0.0.1").getStatus()).isEqualTo(200);
        }
        assertThat(pedir("/api/burger-joints", "181.0.0.1").getStatus()).isEqualTo(200);
    }
}
