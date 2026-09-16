package com.kavanamecania.mecania.domain.chat;

/**
 * Modelo de lenguaje para generar respuestas a partir de un contexto.
 * La interfaz vive en domain; la implementación (OpenRouter) en infraestructura.
 */
public interface LlmService {

    /**
     * Genera una respuesta con un prompt de sistema y un prompt de usuario.
     *
     * @param promptSistema instrucciones de comportamiento del asistente
     * @param promptUsuario contexto + pregunta del usuario
     * @return texto de la respuesta
     * @throws LlmException si el proveedor falla o devuelve una respuesta inválida
     */
    String responder(String promptSistema, String promptUsuario) throws LlmException;
}
