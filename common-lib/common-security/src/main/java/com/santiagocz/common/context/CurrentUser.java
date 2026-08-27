package com.santiagocz.common.context;

import com.santiagocz.common.auth.AuthenticatedUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {}

    public static AuthenticatedUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getDetails() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("No hay usuario autenticado");
        }
        return user;
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }
}