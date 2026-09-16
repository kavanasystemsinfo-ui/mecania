package com.kavanamecania.mecania.domain.vector;

/**
 * Fragmento recuperado por similitud junto con su puntuación.
 * {@code similitud} es el coseno (1 - distancia coseno de pgvector):
 * 1.0 = idéntico, 0.0 = ortogonal.
 */
public record FragmentoSimilar(
        Long fragmentoId,
        Long documentoId,
        int posicion,
        String texto,
        double similitud
) {
}
