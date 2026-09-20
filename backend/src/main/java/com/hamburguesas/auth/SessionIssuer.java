package com.hamburguesas.auth;

import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.model.User;
import com.hamburguesas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Turns "this is who you are" into a live session.
 *
 * Every way into the app ends here, so the cookies are built the same way whether
 * the person signed in with a password, a code, or Google. The response body carries
 * only who the user is: the tokens go in cookies the browser will not show to
 * JavaScript, which is the whole reason for the change.
 */
@Component
@RequiredArgsConstructor
public class SessionIssuer {

    private final JwtService jwtService;
    private final SessionService sessionService;
    private final SessionCookies cookies;

    public ResponseEntity<AuthResponse> start(User user) {
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());
        SessionService.IssuedToken refreshToken = sessionService.issue(user);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookies.access(accessToken).toString())
            .header(HttpHeaders.SET_COOKIE, cookies.refresh(refreshToken.value()).toString())
            .body(new AuthResponse(user.getId(), user.getName(), user.getEmail()));
    }

    /** Used by the refresh endpoint, which already has its next refresh token. */
    public ResponseEntity<AuthResponse> renew(User user, SessionService.IssuedToken refreshToken) {
        String accessToken = jwtService.generateToken(user.getId(), user.getEmail());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, cookies.access(accessToken).toString())
            .header(HttpHeaders.SET_COOKIE, cookies.refresh(refreshToken.value()).toString())
            .body(new AuthResponse(user.getId(), user.getName(), user.getEmail()));
    }

    public ResponseEntity<Void> end() {
        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
            .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
            .build();
    }
}
