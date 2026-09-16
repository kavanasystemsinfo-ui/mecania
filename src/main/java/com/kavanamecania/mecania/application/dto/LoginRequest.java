package com.kavanamecania.mecania.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Inicio de sesión.
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
