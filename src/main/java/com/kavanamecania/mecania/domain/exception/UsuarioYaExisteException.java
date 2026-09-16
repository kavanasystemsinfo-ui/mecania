package com.kavanamecania.mecania.domain.exception;

/**
 * El email ya está registrado.
 */
public class UsuarioYaExisteException extends RuntimeException {
    public UsuarioYaExisteException(String email) {
        super("Ya existe un usuario con el email " + email);
    }
}
