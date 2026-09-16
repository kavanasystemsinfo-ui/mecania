package com.kavanamecania.mecania.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Pregunta del usuario al asistente RAG del vehículo.
 */
public record PreguntaRequest(
        @NotBlank String pregunta
) {
}
