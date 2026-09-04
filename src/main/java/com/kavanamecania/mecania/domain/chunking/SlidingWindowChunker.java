package com.kavanamecania.mecania.domain.chunking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chunker de ventana deslizante por caracteres.
 * Corta el texto en chunks de {@code chunkSize} con {@code overlap} caracteres de solapamiento.
 *
 * <p>Algoritmo: empieza en posición 0, toma el siguiente bloque de {@code chunkSize} chars,
 * salta hacia atrás {@code overlap} para que el siguiente chunk comparta ese tramo, y repite.
 * El último chunk puede ser más corto que {@code chunkSize} si el texto no llena exacto.</p>
 *
 * <p>Es determinista: misma entrada, misma salida. No usa Random, ni Locale, ni dependencias externas.</p>
 *
 * <p>Para nuestro caso de uso (manuales técnicos en español/inglés), chunkSize=512 y overlap=64
 * son los valores por defecto razonables. Ver ADR 004.</p>
 */
public class SlidingWindowChunker implements Chunker {

    private final int chunkSize;
    private final int overlap;

    public SlidingWindowChunker(int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize debe ser > 0, fue " + chunkSize);
        }
        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap debe estar en [0, chunkSize), fue " + overlap + " con chunkSize=" + chunkSize);
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    @Override
    public List<String> chunk(String texto) {
        if (texto == null || texto.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int length = texto.length();
        for (int start = 0; start < length; start += step) {
            int end = Math.min(start + chunkSize, length);
            chunks.add(texto.substring(start, end));
            if (end == length) {
                break;
            }
        }
        return chunks;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getOverlap() {
        return overlap;
    }
}