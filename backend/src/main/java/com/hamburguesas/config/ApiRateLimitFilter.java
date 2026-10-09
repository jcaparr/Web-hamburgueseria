package com.hamburguesas.config;

import com.hamburguesas.auth.RateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Un tope de pedidos por dirección para toda la API (#201).
 *
 * Hasta acá solo /api/auth tenía uno, porque es lo que sirve para adivinar contraseñas.
 * El resto quedaba abierto, y el servidor tiene un solo núcleo: un script pidiendo sin
 * parar alcanzaba para hacer lenta la web para todos. Esto no frena a nadie que la use
 * en serio y sí a quien la quiera ahogar.
 *
 * Las fotos quedan afuera. No tocan la base, el navegador las guarda por semanas, y una
 * pantalla de Explorar o del feed pide veinte de una vez: contarlas obligaría a un tope
 * tan alto que dejaría de frenar nada.
 *
 * El orden importa, y está probado en {@code LimitePorDireccionRealTest}:
 * <ul>
 *   <li>Después del filtro que lee la dirección real que manda Caddy, que va primero de
 *   todos. Antes de él, todos los pedidos tendrían la dirección de Caddy y la web
 *   entera compartiría un solo cupo.</li>
 *   <li>Antes de Spring Security. Un pedido que la seguridad rechaza no llegaba a
 *   contarse, así que una ráfaga contra cualquier camino con sesión pasaba sin freno.
 *   Así se corta antes de validar ninguna sesión.</li>
 * </ul>
 */
@Component
@Order(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1)
@Slf4j
public class ApiRateLimitFilter extends OncePerRequestFilter {

    /**
     * Cuántos pedidos por minuto puede hacer una dirección.
     *
     * Una persona navegando hace unos veinte: cada pantalla pide dos o tres cosas. Pero
     * detrás de una misma dirección puede haber mucha gente —las redes de celular
     * comparten una entre muchos (#148)—, así que el tope deja lugar para una docena
     * navegando a la vez. Un script sin freno hace eso en un segundo.
     *
     * El cupo se recarga de a poco y no de golpe cada minuto: se puede gastar entero en
     * una ráfaga, y después vuelve a razón de cinco por segundo.
     */
    static final int PEDIDOS_POR_MINUTO = 300;

    private static final String[] SIN_TOPE = {"/api/place-photos/", "/api/rating-photos/"};

    private final RateLimiter rateLimiter;
    private final int cupo;

    @Autowired
    public ApiRateLimitFilter(RateLimiter rateLimiter) {
        this(rateLimiter, PEDIDOS_POR_MINUTO);
    }

    /** Con otro cupo, para los tests. */
    ApiRateLimitFilter(RateLimiter rateLimiter, int cupo) {
        this.rateLimiter = rateLimiter;
        this.cupo = cupo;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String camino = request.getRequestURI();
        if (!camino.startsWith("/api/")) {
            return true;
        }
        for (String prefijo : SIN_TOPE) {
            if (camino.startsWith(prefijo)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String direccion = request.getRemoteAddr();

        if (!rateLimiter.tryAcquire("api:" + direccion, cupo, Duration.ofMinutes(1))) {
            // Un aviso por dirección por minuto y no uno por pedido: durante una ráfaga
            // serían cientos por segundo, y el log se llenaría justo cuando hace falta leerlo.
            if (rateLimiter.tryAcquire("aviso-api:" + direccion, 1, Duration.ofMinutes(1))) {
                log.warn("Tope de pedidos a la API alcanzado por una dirección, en {}", request.getRequestURI());
            }
            rechazar(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", "60");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"status\":429,\"error\":\"Demasiados pedidos seguidos. Esperá un minuto y probá de nuevo.\","
                + "\"code\":\"RATE_LIMITED\"}");
    }
}
