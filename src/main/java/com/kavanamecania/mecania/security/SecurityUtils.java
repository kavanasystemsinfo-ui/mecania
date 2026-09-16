package com.kavanamecania.mecania.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Acceso a la identidad del usuario autenticado desde cualquier capa.
 */
public final class SecurityUtils {

    /**
     * Id usado en los tests de lógica de negocio (auth deshabilitada), que
     * coincide con el usuario de los datos de prueba. En producción auth
     * siempre está habilitada, así que este fallback nunca se alcanza.
     */
    private static final Long USUARIO_DEMO = 1L;

    private SecurityUtils() {
    }

    /**
     * Id del usuario autenticado. Si no hay autenticación (tests con auth
     * deshabilitada) devuelve el usuario demo.
     */
    public static Long usuarioIdActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long id) {
            return id;
        }
        return USUARIO_DEMO;
    }
}
