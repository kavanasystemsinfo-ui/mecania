package com.kavanamecania.mecania.domain.exception;

public class AlertaNotFoundException extends RuntimeException {
    public AlertaNotFoundException(Long vehiculoId, Long alertaId) {
        super("Alerta no encontrada: vehiculoId=" + vehiculoId + ", alertaId=" + alertaId);
    }
}
