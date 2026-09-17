package com.hamburguesas.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;

@Getter
public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    private final Long userId;
    private final String name;

    public UserPrincipal(com.hamburguesas.model.User user, List<GrantedAuthority> authorities) {
        super(user.getEmail(), user.getPasswordHash(), authorities);
        this.userId = user.getId();
        this.name = user.getName();
    }
}
