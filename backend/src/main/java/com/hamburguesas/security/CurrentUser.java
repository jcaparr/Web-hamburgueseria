package com.hamburguesas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    public static Long idOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getUserId();
    }

    public static Long requireId() {
        Long id = idOrNull();
        if (id == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return id;
    }
}
