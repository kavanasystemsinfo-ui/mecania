package com.kavanamecania.mecania.infrastructure.extraction;

import com.kavanamecania.mecania.domain.extraction.DocumentoTipo;
import com.kavanamecania.mecania.domain.extraction.ExtractionException;
import com.kavanamecania.mecania.domain.extraction.TextExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Extractor para DOCX usando Apache POI 5.x (paquete poi-ooxml).
 * Solo soporta .docx (formato moderno), no .doc antiguo.
 */
@Component
public class PoiTextExtractor implements TextExtractor {

    @Override
    public boolean soporta(DocumentoTipo tipo) {
        return tipo == DocumentoTipo.DOCX;
    }

    @Override
    public String extraer(byte[] contenido) throws ExtractionException {
        if (contenido == null || contenido.length == 0) {
            throw new ExtractionException("Contenido DOCX nulo o vacío");
        }
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(contenido));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        } catch (IOException e) {
            throw new ExtractionException("No se pudo parsear el DOCX: " + e.getMessage(), e);
        }
    }
}