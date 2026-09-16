package com.kavanamecania.mecania.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Alta de un usuario.
 */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password
) {
}
