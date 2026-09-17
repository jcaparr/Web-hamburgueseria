package com.hamburguesas.security;

import com.hamburguesas.model.Usuario;
import com.hamburguesas.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserPrincipal loadUserByUsername(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new UserPrincipal(usuario, Collections.emptyList());
    }

    public UserPrincipal loadById(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return new UserPrincipal(usuario, Collections.emptyList());
    }
}
