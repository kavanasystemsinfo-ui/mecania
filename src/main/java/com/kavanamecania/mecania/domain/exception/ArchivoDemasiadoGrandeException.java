package com.kavanamecania.mecania.domain.exception;

/**
 * El archivo subido supera el límite configurado para la aplicación.
 *
 * <p>Es un error del usuario, no del servidor: se traduce a 413 con el límite
 * en el mensaje. Existe como excepción propia para que el contrato HTTP no
 * dependa de cómo se llame la excepción de la librería de turno.</p>
 */
public class ArchivoDemasiadoGrandeException extends RuntimeException {

    public ArchivoDemasiadoGrandeException(String mensaje) {
        super(mensaje);
    }
}
