package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.model.Combustible;
import jakarta.validation.constraints.*;

import java.time.Instant;

public record VehiculoDto(
        Long id,
        @NotNull Long usuarioId,
        @NotBlank @Size(max = 50) String marca,
        @NotBlank @Size(max = 100) String modelo,
        @NotNull @Min(1900) @Max(2100) Integer anio,
        @NotNull Combustible combustible,
        @PositiveOrZero Long kilometraje,
        @Size(max = 20) String matricula,
        Instant createdAt
) {}
