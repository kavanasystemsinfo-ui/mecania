package com.kavanamecania.mecania.domain.chat;

/**
 * Fallo al llamar al modelo de lenguaje (proveedor caído, timeout, respuesta
 * inválida). Checked: quien responde a una pregunta debe decidir cómo
 * informar, no dejar que un error pase como respuesta válida.
 */
public class LlmException extends Exception {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
