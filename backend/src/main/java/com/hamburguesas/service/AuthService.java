package com.hamburguesas.service;

import com.hamburguesas.dto.AuthResponse;
import com.hamburguesas.dto.LoginRequest;
import com.hamburguesas.dto.RegisterRequest;
import com.hamburguesas.exception.ConflictException;
import com.hamburguesas.model.Usuario;
import com.hamburguesas.repository.UsuarioRepository;
import com.hamburguesas.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new ConflictException("Ya existe una cuenta con ese email");
        }

        Usuario usuario = Usuario.builder()
            .nombre(request.nombre())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();

        usuario = usuarioRepository.save(usuario);

        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponse(token, usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado tras autenticar"));

        String token = jwtService.generarToken(usuario.getId(), usuario.getEmail());
        return new AuthResponse(token, usuario.getId(), usuario.getNombre(), usuario.getEmail());
    }
}
