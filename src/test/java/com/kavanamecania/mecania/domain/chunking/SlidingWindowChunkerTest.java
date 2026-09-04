package com.kavanamecania.mecania.domain.chunking;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SlidingWindowChunkerTest {

    @Test
    void chunk_con_texto_null_devuelve_lista_vacia() {
        Chunker chunker = new SlidingWindowChunker(512, 64);
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void chunk_con_texto_vacio_devuelve_lista_vacia() {
        Chunker chunker = new SlidingWindowChunker(512, 64);
        assertThat(chunker.chunk("")).isEmpty();
    }

    @Test
    void chunk_con_texto_mas_corto_que_chunk_devuelve_un_solo_chunk() {
        Chunker chunker = new SlidingWindowChunker(100, 10);
        String texto = "texto corto";
        List<String> result = chunker.chunk(texto);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(texto);
    }

    @Test
    void chunk_con_texto_del_tamaño_exacto_devuelve_un_solo_chunk() {
        Chunker chunker = new SlidingWindowChunker(10, 2);
        String texto = "abcdefghij"; // 10 chars exactos
        List<String> result = chunker.chunk(texto);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(texto);
    }

    @Test
    void chunk_con_texto_largo_produce_varios_chunks_con_overlap() {
        // chunkSize=10, overlap=2, step=8
        Chunker chunker = new SlidingWindowChunker(10, 2);
        String texto = "abcdefghijklmnopqrstuvwxyz"; // 26 chars
        List<String> result = chunker.chunk(texto);
        // chunks esperados (start, end):
        // [0,10)  = abcdefghij
        // [8,18)  = ijklmnopqr
        // [16,26) = qrstuvwxyz  (length 10, llega al final)
        assertThat(result).containsExactly(
                "abcdefghij",
                "ijklmnopqr",
                "qrstuvwxyz"
        );
    }

    @Test
    void chunks_son_deterministas() {
        Chunker chunker = new SlidingWindowChunker(50, 5);
        String texto = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";
        List<String> a = chunker.chunk(texto);
        List<String> b = chunker.chunk(texto);
        assertThat(a).isEqualTo(b);
    }

    @Test
    void chunks_respeta_el_tamaño_maximo() {
        Chunker chunker = new SlidingWindowChunker(100, 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) sb.append("0123456789");
        String texto = sb.toString();

        List<String> chunks = chunker.chunk(texto);
        assertThat(chunks).isNotEmpty();
        // Todos los chunks menos el último tienen exactamente chunkSize
        for (int i = 0; i < chunks.size() - 1; i++) {
            assertThat(chunks.get(i)).hasSize(100);
        }
        // El último chunk es <= chunkSize
        assertThat(chunks.get(chunks.size() - 1).length()).isLessThanOrEqualTo(100);
    }

    @Test
    void constructor_rechaza_overlap_mayor_o_igual_a_chunkSize() {
        assertThatThrownBy(() -> new SlidingWindowChunker(100, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlidingWindowChunker(100, 200))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rechaza_overlap_negativo() {
        assertThatThrownBy(() -> new SlidingWindowChunker(100, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rechaza_chunkSize_cero_o_negativo() {
        assertThatThrownBy(() -> new SlidingWindowChunker(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SlidingWindowChunker(-5, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void chunk_con_overlap_cero_produce_chunks_disjuntos() {
        Chunker chunker = new SlidingWindowChunker(5, 0);
        String texto = "abcdefghij"; // 10 chars
        List<String> result = chunker.chunk(texto);
        assertThat(result).containsExactly("abcde", "fghij");
    }
}