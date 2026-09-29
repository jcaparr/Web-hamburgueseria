package com.hamburguesas.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.UUID;

@Getter
public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    /**
     * Stand-in for accounts that only sign in with Google and therefore have no
     * password. It is a real BCrypt hash, so the password check runs normally and takes
     * the usual time, but it hashes a random value nobody has ever seen: no input can
     * match it. Spring's User refuses a null password, and a fixed dummy string would
     * be a password that someone could eventually type.
     */
    private static final String UNUSABLE_PASSWORD =
        new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());

    private final Long userId;

    public UserPrincipal(com.hamburguesas.model.User user, List<GrantedAuthority> authorities) {
        // Deliberately always enabled. Marking unverified accounts as disabled would be
        // the obvious move, but Spring checks that flag *before* the password, which
        // would let anyone discover which emails are registered without knowing it.
        // AuthService.login checks verification after the password instead.
        super(user.getEmail(),
            user.getPasswordHash() != null ? user.getPasswordHash() : UNUSABLE_PASSWORD,
            authorities);
        this.userId = user.getId();
    }
}
