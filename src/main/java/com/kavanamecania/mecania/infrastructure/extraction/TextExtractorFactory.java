package com.kavanamecania.mecania.infrastructure.extraction;

import com.kavanamecania.mecania.domain.extraction.DocumentoTipo;
import com.kavanamecania.mecania.domain.extraction.ExtractionException;
import com.kavanamecania.mecania.domain.extraction.TextExtractor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Selecciona el TextExtractor adecuado según el tipo de documento.
 * Spring inyecta todos los beans que implementen TextExtractor.
 */
@Component
public class TextExtractorFactory {

    private final List<TextExtractor> extractors;

    public TextExtractorFactory(List<TextExtractor> extractors) {
        this.extractors = extractors;
    }

    public TextExtractor para(DocumentoTipo tipo) {
        return extractors.stream()
                .filter(e -> e.soporta(tipo))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay TextExtractor para el tipo: " + tipo));
    }

    /**
     * Convenience: extrae texto directamente del tipo y los bytes.
     */
    public String extraer(DocumentoTipo tipo, byte[] contenido) throws ExtractionException {
        return para(tipo).extraer(contenido);
    }
}