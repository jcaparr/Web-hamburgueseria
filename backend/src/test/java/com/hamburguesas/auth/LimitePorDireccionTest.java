package com.hamburguesas.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El límite por dirección de /api/auth (#148): frena lo que sirve para adivinar o para
 * hacer mandar mails, y no lo que una sesión pide sola al cargar cada página.
 */
class LimitePorDireccionTest {

    /** Chico a propósito, para no tener que hacer treinta pedidos en cada test. */
    private static final int CUPO = 3;

    private AuthRateLimitFilter filtro;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.getRateLimit().setPerIpRequests(CUPO);
        properties.getRateLimit().setPerIpWindowMinutes(15);
        filtro = new AuthRateLimitFilter(new RateLimiter(), properties);
    }

    /** El código de la respuesta a un pedido desde la misma dirección de siempre. */
    private int pedir(String metodo, String camino) throws Exception {
        MockHttpServletRequest pedido = new MockHttpServletRequest(metodo, camino);
        pedido.setRemoteAddr("181.0.0.1");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(pedido, respuesta, new MockFilterChain());
        return respuesta.getStatus();
    }

    @Test
    void elLoginSigueLimitado() throws Exception {
        for (int i = 0; i < CUPO; i++) {
            assertThat(pedir("POST", "/api/auth/login")).isEqualTo(200);
        }
        assertThat(pedir("POST", "/api/auth/login")).isEqualTo(429);
    }

    /**
     * Lo que se reprodujo en #148: muchas cargas de página seguidas desde la misma
     * dirección, y después alguien que quiere entrar.
     */
    @Test
    void preguntarDeQuienEsLaSesionNoGastaElCupoDelLogin() throws Exception {
        for (int i = 0; i < CUPO * 20; i++) {
            assertThat(pedir("GET", "/api/auth/me")).isEqualTo(200);
        }
        assertThat(pedir("POST", "/api/auth/login")).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", "/api/auth/refresh", "/api/auth/logout"})
    void loDeLaSesionNoTieneLimite(String camino) throws Exception {
        for (int i = 0; i < CUPO * 20; i++) {
            assertThat(pedir("POST", camino)).isEqualTo(200);
        }
    }

    /** Todo lo que sirve para adivinar una contraseña o hace mandar un mail. */
    @ParameterizedTest
    @ValueSource(strings = {
        "/api/auth/register", "/api/auth/verify-email", "/api/auth/resend-code",
        "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/google",
        "/api/auth/username-available"})
    void loQueSirveParaAdivinarOMandarMailsSigueLimitado(String camino) throws Exception {
        for (int i = 0; i < CUPO; i++) {
            pedir("POST", camino);
        }
        assertThat(pedir("POST", camino)).isEqualTo(429);
    }

    /**
     * La excepción es por camino exacto. Una variante de /me no llega a /me, así que no
     * tiene por qué saltearse el límite.
     */
    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me/", "/api/auth/mex", "/api/auth/me/../login"})
    void unaVarianteDeUnCaminoDeLaSesionSigueContando(String camino) throws Exception {
        for (int i = 0; i < CUPO; i++) {
            pedir("POST", camino);
        }
        assertThat(pedir("POST", camino)).isEqualTo(429);
    }

    @Test
    void loQueNoEsDeAuthNoPasaPorElLimite() throws Exception {
        for (int i = 0; i < CUPO * 20; i++) {
            assertThat(pedir("GET", "/api/burger-joints")).isEqualTo(200);
        }
    }
}
