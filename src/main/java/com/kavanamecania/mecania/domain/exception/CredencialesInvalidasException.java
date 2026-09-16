package com.kavanamecania.mecania.domain.exception;

/**
 * Email o contraseña incorrectos en el login.
 */
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() {
        super("Email o contraseña incorrectos");
    }
}
