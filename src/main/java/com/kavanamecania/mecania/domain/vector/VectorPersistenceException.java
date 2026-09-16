package com.kavanamecania.mecania.domain.vector;

/**
 * Fallo al persistir o recuperar embeddings en el almacén de vectores.
 * Checked a propósito: quien procesa un documento debe decidir qué hacer
 * (marcar ERROR y revertir) y no puede ignorarlo por accidente.
 */
public class VectorPersistenceException extends Exception {

    public VectorPersistenceException(String message) {
        super(message);
    }

    public VectorPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
