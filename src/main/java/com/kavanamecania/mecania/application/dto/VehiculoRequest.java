package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.model.Combustible;
import jakarta.validation.constraints.*;

/**
 * Alta/edición de un vehículo. El propietario ya no se envía en el body:
 * lo determina el token JWT del usuario autenticado.
 */
public record VehiculoRequest(
        @NotBlank @Size(max = 50) String marca,
        @NotBlank @Size(max = 100) String modelo,
        @NotNull @Min(1900) @Max(2100) Integer anio,
        @NotNull Combustible combustible,
        @PositiveOrZero Long kilometraje,
        @Size(max = 20) String matricula
) {}
