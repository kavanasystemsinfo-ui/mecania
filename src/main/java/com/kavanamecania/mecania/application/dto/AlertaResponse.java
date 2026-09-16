package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.model.Alerta;

import java.time.LocalDate;

public record AlertaResponse(
        Long id,
        String tipo,
        String descripcion,
        LocalDate fecha,
        String repetitividad,
        boolean activa
) {
    public static AlertaResponse from(Alerta a) {
        return new AlertaResponse(
                a.getId(),
                a.getTipo() != null ? a.getTipo().name() : null,
                a.getDescripcion(),
                a.getFecha(),
                a.getRepetitividad() != null ? a.getRepetitividad().name() : null,
                a.isActiva()
        );
    }
}
