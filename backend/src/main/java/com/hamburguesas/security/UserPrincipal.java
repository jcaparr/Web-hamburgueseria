package com.hamburguesas.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@Getter
public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final String name;

    public UserPrincipal(com.hamburguesas.model.User user, List<GrantedAuthority> authorities) {
        // Deliberately always enabled. Marking unverified accounts as disabled would be
        // the obvious move, but Spring checks that flag *before* the password, which
        // would let anyone discover which emails are registered without knowing it.
        // AuthService.login checks verification after the password instead.
        super(user.getEmail(), user.getPasswordHash(), authorities);
        this.userId = user.getId();
        this.name = user.getName();
    }
}
