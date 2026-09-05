package com.kavanamecania.mecania.domain.embedding;

/**
 * Servicio de embeddings. Convierte texto a un vector denso de doubles.
 *
 * <p>La interfaz vive en domain porque "qué queremos" (texto -> vector) es lógica de negocio.
 * La implementación (HTTP a OpenRouter, llamada a un modelo local, etc.) vive en infraestructura.</p>
 */
public interface EmbeddingService {

    /**
     * Calcula el embedding de un texto.
     *
     * @param texto texto a vectorizar. No debe ser null ni vacío.
     * @return embedding con la dimensionalidad del modelo configurado
     * @throws EmbeddingException si falla la llamada al proveedor o el texto es inválido
     */
    Embedding embed(String texto) throws EmbeddingException;

    /**
     * Dimensionalidad del embedding que produce esta implementación.
     * Útil para validar que el tipo de columna en BD coincide con el modelo.
     */
    int dimension();
}