package com.hamburguesas.auth;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

/**
 * Builds the session cookies, and is the one place their security attributes are set.
 *
 * HttpOnly is the point of the whole change: tokens used to live in localStorage,
 * where any injected script could read them. A cookie the browser will not hand to
 * JavaScript cannot be stolen that way.
 */
@Component
@RequiredArgsConstructor
public class SessionCookies {

    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    private final AuthProperties properties;

    public ResponseCookie access(String token) {
        return base(ACCESS_COOKIE, token)
            .path("/api")
            .maxAge(Duration.ofMinutes(properties.getSession().getAccessTokenMinutes()))
            .build();
    }

    public ResponseCookie refresh(String token) {
        return base(REFRESH_COOKIE, token)
            // Narrower than the access cookie: the refresh token is only ever sent to
            // the endpoints that use it, so it is not attached to every API call.
            .path("/api/auth")
            .maxAge(Duration.ofDays(properties.getSession().getRefreshTokenDays()))
            .build();
    }

    /** Same attributes, empty value, immediate expiry: anything else is not removed. */
    public ResponseCookie clearAccess() {
        return base(ACCESS_COOKIE, "").path("/api").maxAge(Duration.ZERO).build();
    }

    public ResponseCookie clearRefresh() {
        return base(REFRESH_COOKIE, "").path("/api/auth").maxAge(Duration.ZERO).build();
    }

    public Optional<String> read(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
            .filter(cookie -> name.equals(cookie.getName()))
            .map(jakarta.servlet.http.Cookie::getValue)
            .filter(value -> value != null && !value.isBlank())
            .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder base(String name, String value) {
        return ResponseCookie.from(name, value)
            .httpOnly(true)
            .secure(properties.getSession().isCookieSecure())
            // Strict, not Lax: the app and the API share an origin, so nothing
            // legitimate arrives from another site. This is also what makes CSRF a
            // non-issue for the endpoints that rely on the cookie alone.
            .sameSite("Strict");
    }
}
