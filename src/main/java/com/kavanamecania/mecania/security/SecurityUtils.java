package com.kavanamecania.mecania.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso a la identidad del usuario autenticado desde cualquier capa.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Id del usuario autenticado, o {@code null} si la petición no va
     * autenticada (por ejemplo, en tests con autenticación deshabilitada).
     */
    public static Long usuarioIdActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return id;
        }
        return null;
    }
}
