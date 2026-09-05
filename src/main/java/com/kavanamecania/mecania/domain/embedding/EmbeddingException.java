package com.kavanamecania.mecania.domain.embedding;

/**
 * Error al calcular un embedding. El procesador async captura esta excepción y marca el documento como ERROR.
 */
public class EmbeddingException extends Exception {
    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}