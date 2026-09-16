package com.kavanamecania.mecania.application.dto;

import com.kavanamecania.mecania.domain.vector.FragmentoSimilar;

/**
 * Fuente (fragmento de manual) que sostiene una respuesta del chat.
 * Se devuelve para que el usuario pueda verificar de dónde sale cada dato.
 */
public record FuenteRespuesta(
        Long documentoId,
        int posicion,
        String texto,
        double similitud
) {
    public static FuenteRespuesta from(FragmentoSimilar f) {
        return new FuenteRespuesta(f.documentoId(), f.posicion(), f.texto(), f.similitud());
    }
}
