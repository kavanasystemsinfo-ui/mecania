package com.kavanamecania.mecania.domain.exception;

public class VehiculoDuplicadoException extends RuntimeException {
    public VehiculoDuplicadoException(Long usuarioId, String marca, String modelo, Integer anio) {
        super("Vehículo duplicado para usuarioId=" + usuarioId
                + " (" + marca + " " + modelo + " " + anio + ")");
    }
}
