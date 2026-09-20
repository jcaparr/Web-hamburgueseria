package com.hamburguesas.security;

import com.hamburguesas.auth.SessionCookies;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SessionCookies sessionCookies;

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // The cookie is the only place the browser keeps a token now. The Authorization
        // header is still read for anything that is not a browser, but nothing in this
        // app sends it any more.
        Optional<String> token = sessionCookies
            .read(request, SessionCookies.ACCESS_COOKIE)
            .or(() -> bearerHeader(request));

        if (token.isPresent()) {
            try {
                Long userId = jwtService.extractUserId(token.get());
                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserPrincipal principal = userDetailsService.loadById(userId);
                    var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, principal.getAuthorities()
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (JwtException | IllegalArgumentException | AuthenticationException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> bearerHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ")
            ? Optional.of(header.substring(7))
            : Optional.empty();
    }
}
