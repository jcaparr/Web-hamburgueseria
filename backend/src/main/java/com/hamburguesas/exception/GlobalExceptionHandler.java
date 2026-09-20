package com.hamburguesas.exception;

import com.hamburguesas.auth.SessionRejectedException;
import com.hamburguesas.mail.EmailDeliveryException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        return body(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(ConflictException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
    }

    /**
     * Carries a machine-readable code so the frontend can send the user to the
     * verification screen instead of just showing the message.
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleNotVerified(EmailNotVerifiedException ex) {
        return body(HttpStatus.FORBIDDEN, ex.getMessage(), "EMAIL_NOT_VERIFIED");
    }

    /**
     * The account exists but belongs to the other sign-in method. Raised only once the
     * caller has proved they own the address, so the message can say what to do.
     */
    @ExceptionHandler(WrongSignInMethodException.class)
    public ResponseEntity<Map<String, Object>> handleWrongMethod(WrongSignInMethodException ex) {
        return body(HttpStatus.CONFLICT, ex.getMessage(), "WRONG_SIGN_IN_METHOD");
    }

    @ExceptionHandler(InvalidCodeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCode(InvalidCodeException ex) {
        return body(HttpStatus.BAD_REQUEST, ex.getMessage(), "INVALID_CODE");
    }

    /**
     * Only reached on registration, where the account is rolled back with it: telling
     * the user to try again is honest, and hiding it would leave them waiting for an
     * email that is never coming.
     */
    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<Map<String, Object>> handleEmailDelivery(EmailDeliveryException ex) {
        return body(HttpStatus.SERVICE_UNAVAILABLE,
            "No pudimos mandarte el email. Probá de nuevo en un rato.", "EMAIL_DELIVERY_FAILED");
    }

    /**
     * The refresh cookie is missing, expired or already used. 401 so the frontend
     * treats it like any other lost session and sends the person to log in.
     */
    @ExceptionHandler(SessionRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleSessionRejected(SessionRejectedException ex) {
        return body(HttpStatus.UNAUTHORIZED, ex.getMessage(), "SESSION_EXPIRED");
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException ex) {
        return body(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), "RATE_LIMITED");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Datos invalidos");
        return body(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return body(status, message, null);
    }

    private ResponseEntity<Map<String, Object>> body(HttpStatus status, String message, String code) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", Instant.now().toString());
        payload.put("status", status.value());
        payload.put("error", message);
        if (code != null) {
            payload.put("code", code);
        }
        return ResponseEntity.status(status).body(payload);
    }
}
