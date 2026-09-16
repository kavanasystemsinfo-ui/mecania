package com.kavanamecania.mecania.application.dto;

/**
 * Respuesta de autenticación: el token JWT y el email del usuario.
 */
public record AuthResponse(
        String token,
        String email
) {
}
