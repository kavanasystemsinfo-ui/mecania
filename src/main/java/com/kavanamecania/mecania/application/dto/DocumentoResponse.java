package com.kavanamecania.mecania.application.dto;

import java.time.Instant;

public record DocumentoResponse(
        Long id,
        Long vehiculoId,
        String nombre,
        String tipo,
        String rutaAlmacenamiento,
        String estado,
        Long tamanoBytes,
        String mensajeError,
        Instant createdAt
) {
    public static DocumentoResponse from(com.kavanamecania.mecania.domain.model.Documento d) {
        return new DocumentoResponse(
                d.getId(),
                d.getVehiculo().getId(),
                d.getNombre(),
                d.getTipo() != null ? d.getTipo().name() : null,
                d.getRutaAlmacenamiento(),
                d.getEstado() != null ? d.getEstado().name() : null,
                d.getTamanoBytes(),
                d.getMensajeError(),
                d.getCreatedAt()
        );
    }
}