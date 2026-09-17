package com.hamburguesas.security;

import com.hamburguesas.model.Usuario;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class UserPrincipal extends User {

    private final Long usuarioId;
    private final String nombre;

    public UserPrincipal(Usuario usuario, List<GrantedAuthority> authorities) {
        super(usuario.getEmail(), usuario.getPasswordHash(), authorities);
        this.usuarioId = usuario.getId();
        this.nombre = usuario.getNombre();
    }
}
