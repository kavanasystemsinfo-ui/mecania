package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.model.Repetitividad;
import com.kavanamecania.mecania.domain.model.TipoAlerta;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Alta o edición de una alerta de mantenimiento.
 */
public record AlertaRequest(
        @NotNull TipoAlerta tipo,
        @NotBlank String descripcion,
        @NotNull LocalDate fecha,
        @NotNull Repetitividad repetitividad
) {
}
