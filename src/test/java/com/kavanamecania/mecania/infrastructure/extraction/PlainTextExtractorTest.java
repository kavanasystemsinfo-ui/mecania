package com.kavanamecania.mecania.infrastructure.extraction;

import com.kavanamecania.mecania.domain.extraction.DocumentoTipo;
import com.kavanamecania.mecania.domain.extraction.ExtractionException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlainTextExtractorTest {

    private final PlainTextExtractor extractor = new PlainTextExtractor();

    @Test
    void soporta_solo_txt() {
        assertThat(extractor.soporta(DocumentoTipo.TXT)).isTrue();
        assertThat(extractor.soporta(DocumentoTipo.PDF)).isFalse();
        assertThat(extractor.soporta(DocumentoTipo.DOCX)).isFalse();
    }

    @Test
    void extraer_devuelve_texto_utf8() throws ExtractionException {
        String original = "Hola mundo. Este es un manual de prueba. Áéíóú.";
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);

        String resultado = extractor.extraer(bytes);

        assertThat(resultado).isEqualTo(original);
    }

    @Test
    void extraer_con_contenido_vacio_lanza_excepcion() {
        assertThatThrownBy(() -> extractor.extraer(new byte[0]))
                .isInstanceOf(ExtractionException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void extraer_con_contenido_nulo_lanza_excepcion() {
        assertThatThrownBy(() -> extractor.extraer(null))
                .isInstanceOf(ExtractionException.class);
    }

    @Test
    void extraer_con_texto_largo_devuelve_todo_el_contenido() throws ExtractionException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Línea ").append(i).append(" con algo de texto adicional.\n");
        }
        String original = sb.toString();
        byte[] bytes = original.getBytes(StandardCharsets.UTF_8);

        String resultado = extractor.extraer(bytes);

        // Comparamos strings (chars), no bytes: los bytes UTF-8 pueden ser más largos que el string.
        assertThat(resultado).isEqualTo(original);
        assertThat(resultado).startsWith("Línea 0");
        assertThat(resultado).contains("Línea 500");
        assertThat(resultado).endsWith("Línea 999 con algo de texto adicional.\n");
    }
}