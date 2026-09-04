package com.kavanamecania.mecania.infrastructure.extraction;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfBoxTextExtractorTest {

    private final PdfBoxTextExtractor extractor = new PdfBoxTextExtractor();

    /**
     * Genera un PDF mínimo en memoria con el texto indicado, sin tocar disco.
     */
    private byte[] generarPdfCon(String texto) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 700);
                cs.showText(texto);
                cs.endText();
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    void extraer_devuelve_texto_del_pdf() throws Exception {
        byte[] pdfBytes = generarPdfCon("Hola Mecania desde un PDF");

        String resultado = extractor.extraer(pdfBytes);

        assertThat(resultado).contains("Hola Mecania");
        assertThat(resultado).contains("PDF");
    }

    @Test
    void extraer_con_pdf_vacio_lanza_excepcion() {
        assertThatThrownBy(() -> extractor.extraer(new byte[0]))
                .isInstanceOf(com.kavanamecania.mecania.domain.extraction.ExtractionException.class);
    }

    @Test
    void extraer_con_bytes_invalidos_lanza_excepcion() {
        byte[] basura = "esto no es un PDF valido".getBytes();
        assertThatThrownBy(() -> extractor.extraer(basura))
                .isInstanceOf(com.kavanamecania.mecania.domain.extraction.ExtractionException.class);
    }

}