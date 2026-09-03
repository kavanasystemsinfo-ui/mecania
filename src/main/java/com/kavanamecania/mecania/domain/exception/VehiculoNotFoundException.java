package com.kavanamecania.mecania.domain.exception;

public class VehiculoNotFoundException extends RuntimeException {
    public VehiculoNotFoundException(Long id) {
        super("Vehículo no encontrado: id=" + id);
    }
}
