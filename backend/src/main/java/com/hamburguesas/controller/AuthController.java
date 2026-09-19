package com.hamburguesas.controller;

import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.EmailOnlyRequest;
import com.hamburguesas.dto.LoginRequest;
import com.hamburguesas.dto.MessageResponse;
import com.hamburguesas.dto.RegisterRequest;
import com.hamburguesas.dto.ResetPasswordRequest;
import com.hamburguesas.dto.VerifyEmailRequest;
import com.hamburguesas.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 202, not 201: the account is not usable until the emailed code is entered, and
     * the answer is the same whether or not the address was already registered.
     */
    @PostMapping("/register")
    public ResponseEntity<MessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.accepted().body(authService.register(request));
    }

    /** The only place a brand-new account gets its first token. */
    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request));
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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
