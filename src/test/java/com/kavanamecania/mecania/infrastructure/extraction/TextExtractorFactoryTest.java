package com.kavanamecania.mecania.infrastructure.extraction;

import com.kavanamecania.mecania.domain.extraction.DocumentoTipo;
import com.kavanamecania.mecania.domain.extraction.ExtractionException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextExtractorFactoryTest {

    private final TextExtractorFactory factory = new TextExtractorFactory(List.of(
            new PlainTextExtractor(),
            new PdfBoxTextExtractor(),
            new PoiTextExtractor()
    ));

    @Test
    void para_devuelve_extractor_correcto_para_txt() {
        assertThat(factory.para(DocumentoTipo.TXT)).isInstanceOf(PlainTextExtractor.class);
    }

    @Test
    void para_devuelve_extractor_correcto_para_pdf() {
        assertThat(factory.para(DocumentoTipo.PDF)).isInstanceOf(PdfBoxTextExtractor.class);
    }

    @Test
    void para_devuelve_extractor_correcto_para_docx() {
        assertThat(factory.para(DocumentoTipo.DOCX)).isInstanceOf(PoiTextExtractor.class);
    }

    @Test
    void extraer_delega_en_el_extractor_correcto() throws ExtractionException {
        byte[] contenido = "Hola mundo".getBytes(StandardCharsets.UTF_8);
        String resultado = factory.extraer(DocumentoTipo.TXT, contenido);
        assertThat(resultado).isEqualTo("Hola mundo");
    }
}