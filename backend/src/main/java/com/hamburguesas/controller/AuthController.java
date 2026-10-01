package com.hamburguesas.controller;

import com.hamburguesas.auth.SessionCookies;
import com.hamburguesas.auth.SessionIssuer;
import com.hamburguesas.auth.SessionRejectedException;
import com.hamburguesas.auth.SessionService;
import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.EmailOnlyRequest;
import com.hamburguesas.dto.GoogleLoginRequest;
import com.hamburguesas.dto.LoginRequest;
import com.hamburguesas.dto.MessageResponse;
import com.hamburguesas.dto.RegisterRequest;
import com.hamburguesas.dto.ResetPasswordRequest;
import com.hamburguesas.dto.UsernameAvailabilityResponse;
import com.hamburguesas.dto.VerifyEmailRequest;
import com.hamburguesas.security.CurrentUser;
import com.hamburguesas.service.AuthService;
import com.hamburguesas.service.GoogleAuthService;
import com.hamburguesas.service.UsernameService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final SessionIssuer sessionIssuer;
    private final SessionService sessionService;
    private final SessionCookies cookies;
    private final UsernameService usernameService;

    /**
     * 202, not 201: the account is not usable until the emailed code is entered.
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.accepted().body(authService.register(request));
    }

    /** The only place a brand-new account gets its first session. */
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return sessionIssuer.start(authService.verifyEmail(request));
    }

    /**
     * Si ese nombre se puede usar, para marcar el campo antes de mandar el formulario.
     *
     * Cuelga de /api/auth/ a propósito: ahí el filtro ya limita por dirección, y esto
     * es justo lo que alguien usaría para barrer qué nombres existen. Por eso también
     * el frontend pregunta al salir del campo y no en cada tecla.
     */
    @GetMapping("/username-available")
    public UsernameAvailabilityResponse usernameAvailable(@RequestParam String username) {
        boolean libre = usernameService.estaLibre(username);
        return new UsernameAvailabilityResponse(
            libre, libre ? "" : usernameService.sugerirCerca(username));
    }

    @PostMapping("/resend-code")
    public ResponseEntity<MessageResponse> resendCode(@Valid @RequestBody EmailOnlyRequest request) {
        return ResponseEntity.accepted().body(authService.resendVerificationCode(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody EmailOnlyRequest request) {
        return ResponseEntity.accepted().body(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request) {
        return sessionIssuer.start(googleAuthService.login(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionIssuer.start(authService.login(request));
    }

    /**
     * Swaps the refresh cookie for a new pair. The access token is deliberately short
     * lived, so the app calls this whenever it expires, and on every page load.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        String presented = cookies.read(request, SessionCookies.REFRESH_COOKIE)
            .orElseThrow(() -> new SessionRejectedException("No hay sesión"));

        SessionService.RotatedSession rotated = sessionService.rotate(presented);
        return sessionIssuer.renew(rotated.user(), rotated.refreshToken());
    }

    /** Ends this session only. Other devices keep theirs. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        cookies.read(request, SessionCookies.REFRESH_COOKIE).ifPresent(sessionService::revoke);
        return sessionIssuer.end();
    }

    /**
     * Who the cookie belongs to. With the tokens out of reach of JavaScript, this is
     * how the app finds out on load whether it is signed in.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me() {
        return authService.quienEs(CurrentUser.requireId())
            .map(user -> ResponseEntity.ok(SessionIssuer.quienEs(user)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
