package com.kavanamecania.mecania.domain.extraction;

/**
 * Error al extraer texto de un archivo.
 * Se eleva al servicio que orquesta el procesamiento, que marca el documento como ERROR.
 */
public class ExtractionException extends Exception {
    public ExtractionException(String message) {
        super(message);
    }

    public ExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}