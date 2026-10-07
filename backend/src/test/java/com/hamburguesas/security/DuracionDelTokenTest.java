package com.hamburguesas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El token de acceso dura lo mismo que la cookie que lo lleva.
 *
 * Antes tenía su propia duración, de 60 minutos, contra 15 de la cookie: un token
 * robado seguía sirviendo cuatro veces más de lo pensado, y un JWT no se puede revocar.
 * El test arma el servicio desde la configuración, como en la app, para que lo que se
 * pruebe sea de dónde sale la duración y no un número puesto a mano.
 */
class DuracionDelTokenTest {

    private static final String SECRETO = "un-secreto-de-prueba-largo-de-sobra-para-hs256";

    @Test
    void duraLoMismoQueLaCookieDeSesion() {
        new ApplicationContextRunner()
            .withPropertyValues(
                "app.jwt.secret=" + SECRETO,
                "app.auth.session.access-token-minutes=15")
            .withBean(JwtService.class)
            .run(contexto -> {
                String token = contexto.getBean(JwtService.class).generateToken(7L, "juan@example.com");

                Claims claims = Jwts.parser()
                    .verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

                Duration vida = Duration.between(claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
                assertThat(vida).isEqualTo(Duration.ofMinutes(15));
            });
    }
}
