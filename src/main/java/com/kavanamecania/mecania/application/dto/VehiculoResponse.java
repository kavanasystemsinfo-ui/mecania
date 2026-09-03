package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.model.Vehiculo;

import java.time.Instant;

public record VehiculoResponse(
        Long id,
        Long usuarioId,
        String marca,
        String modelo,
        Integer anio,
        String combustible,
        Long kilometraje,
        String matricula,
        Instant createdAt
) {
    public static VehiculoResponse from(Vehiculo v) {
        return new VehiculoResponse(
                v.getId(),
                v.getUsuarioId(),
                v.getMarca(),
                v.getModelo(),
                v.getAnio(),
                v.getCombustible() != null ? v.getCombustible().name() : null,
                v.getKilometraje(),
                v.getMatricula(),
                v.getCreatedAt()
        );
    }
}
