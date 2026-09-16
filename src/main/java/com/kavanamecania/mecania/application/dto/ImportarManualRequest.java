package com.kavanamecania.mecania.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * URL de un manual que el usuario ha aceptado importar (normalmente pegada a
 * mano tras revisar los candidatos propuestos).
 */
public record ImportarManualRequest(
        @NotBlank(message = "La URL del manual es obligatoria")
        String url
) {
}
