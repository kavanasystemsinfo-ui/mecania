package com.kavanamecania.mecania.infrastructure.extraction;

import com.kavanamecania.mecania.domain.extraction.DocumentoTipo;
import com.kavanamecania.mecania.domain.extraction.ExtractionException;
import com.kavanamecania.mecania.domain.extraction.TextExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Extractor para PDFs usando Apache PDFBox 3.x.
 * Usa la API moderna (Loader.loadPDF(bytes)) en lugar de PDDocument.load(file).
 */
@Component
public class PdfBoxTextExtractor implements TextExtractor {

    @Override
    public boolean soporta(DocumentoTipo tipo) {
        return tipo == DocumentoTipo.PDF;
    }

    @Override
    public String extraer(byte[] contenido) throws ExtractionException {
        if (contenido == null || contenido.length == 0) {
            throw new ExtractionException("Contenido PDF nulo o vacío");
        }
        try (PDDocument doc = Loader.loadPDF(contenido)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(doc);
        } catch (IOException e) {
            throw new ExtractionException("No se pudo parsear el PDF: " + e.getMessage(), e);
        }
    }
}