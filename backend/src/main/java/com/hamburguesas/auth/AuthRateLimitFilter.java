package com.hamburguesas.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Caps how many auth requests one address can make.
 *
 * The per-address limit lives here, in front of everything, so a flood costs no
 * database work. The per-account limits live in AuthService, where the email has
 * already been parsed and validated.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final AuthProperties properties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var config = properties.getRateLimit();
        // Caddy is the only thing that can reach this app, and Spring rewrites the
        // remote address from the X-Forwarded-For it sets, so this is the real client.
        String key = "ip:" + request.getRemoteAddr();

        if (!rateLimiter.tryAcquire(key, config.getPerIpRequests(),
                Duration.ofMinutes(config.getPerIpWindowMinutes()))) {
            log.warn("Rate limit hit for an address on {}", request.getRequestURI());
            reject(response);
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"status\":429,\"error\":\"Demasiados intentos. Esperá unos minutos.\","
                + "\"code\":\"RATE_LIMITED\"}");
    }
}
