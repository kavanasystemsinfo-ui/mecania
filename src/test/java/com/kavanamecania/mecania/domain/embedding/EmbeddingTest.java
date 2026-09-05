package com.kavanamecania.mecania.domain.embedding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingTest {

    @Test
    void embedding_se_construye_con_valores_y_es_inmutable() {
        double[] valores = {0.1, 0.2, 0.3};
        Embedding e = new Embedding(valores);

        assertThat(e.dimension()).isEqualTo(3);
        // Modificar el array original no afecta al embedding (inmutabilidad defensiva)
        valores[0] = 999.0;
        assertThat(e.valores()[0]).isEqualTo(0.1);
    }

    @Test
    void embedding_con_null_lanza_excepcion() {
        assertThatThrownBy(() -> new Embedding(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void embedding_con_array_vacio_lanza_excepcion() {
        assertThatThrownBy(() -> new Embedding(new double[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toPgVectorString_genera_formato_pgvector() {
        Embedding e = new Embedding(new double[]{0.1, 0.2, 0.3});
        assertThat(e.toPgVectorString()).isEqualTo("[0.1,0.2,0.3]");
    }

    @Test
    void embedding_equals_por_contenido() {
        Embedding a = new Embedding(new double[]{0.1, 0.2});
        Embedding b = new Embedding(new double[]{0.1, 0.2});
        Embedding c = new Embedding(new double[]{0.1, 0.3});
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
    }
}