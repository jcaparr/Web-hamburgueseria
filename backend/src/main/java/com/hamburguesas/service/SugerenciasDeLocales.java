package com.hamburguesas.service;

import com.hamburguesas.dto.SugerenciaDeLocalRequest;
import com.hamburguesas.exception.ResourceNotFoundException;
import com.hamburguesas.exception.TooManyRequestsException;
import com.hamburguesas.model.SugerenciaDeLocal;
import com.hamburguesas.model.User;
import com.hamburguesas.repository.SugerenciaDeLocalRepository;
import com.hamburguesas.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Guarda los locales que la gente avisa que faltan (#257).
 *
 * No llama a Google de ninguna forma: es texto que Juan revisa a mano. Pide sesión para
 * frenar el spam sin captcha, y el tope por persona es para que una cuenta sola no llene
 * la tabla.
 */
@Service
@RequiredArgsConstructor
public class SugerenciasDeLocales {

    /** Cinco por persona en las últimas 24 horas. Ventana corrida, no día calendario. */
    static final int TOPE_POR_DIA = 5;

    private final SugerenciaDeLocalRepository sugerencias;
    private final UserRepository userRepository;

    @Transactional
    public void sugerir(Long userId, SugerenciaDeLocalRequest pedido) {
        Instant ahora = Instant.now();
        long ultimas = sugerencias.countByUser_IdAndCreatedAtAfter(userId, ahora.minus(Duration.ofDays(1)));
        if (ultimas >= TOPE_POR_DIA) {
            throw new TooManyRequestsException("Llegaste a " + TOPE_POR_DIA
                + " sugerencias por hoy. Probá de nuevo mañana.");
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        sugerencias.save(SugerenciaDeLocal.builder()
            .user(user)
            .nombre(pedido.nombre().strip())
            .direccion(pedido.direccion().strip())
            .email(pedido.email().strip())
            .comentario(vacioANulo(pedido.comentario()))
            .createdAt(ahora)
            .build());
    }

    private static String vacioANulo(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.strip();
    }
}
