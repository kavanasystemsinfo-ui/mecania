package com.kavanamecania.mecania.domain.chunking;

import java.util.List;

/**
 * Estrategia para trocear texto en fragmentos.
 * El chunker NO sabe de embeddings ni de documentos; solo corta texto.
 */
public interface Chunker {

    /**
     * @param texto texto a trocear. Si está vacío o es null, devuelve lista vacía.
     * @return lista de chunks en orden de aparición. Cada chunk tiene length <= tamaño máximo.
     */
    List<String> chunk(String texto);
}