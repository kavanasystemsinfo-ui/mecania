package com.kavanamecania.mecania.infrastructure.extraction;

import com.kavanamecania.mecania.domain.extraction.DocumentoTipo;
import com.kavanamecania.mecania.domain.extraction.ExtractionException;
import com.kavanamecania.mecania.domain.extraction.TextExtractor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Extractor para archivos de texto plano.
 * Asume UTF-8; si el archivo no es UTF-8 válido, los caracteres ilegibles se sustituyen.
 */
@Component
public class PlainTextExtractor implements TextExtractor {

    @Override
    public boolean soporta(DocumentoTipo tipo) {
        return tipo == DocumentoTipo.TXT;
    }

    @Override
    public String extraer(byte[] contenido) throws ExtractionException {
        if (contenido == null) {
            throw new ExtractionException("Contenido nulo");
        }
        if (contenido.length == 0) {
            throw new ExtractionException("Contenido TXT vacío");
        }
        try {
            return new String(contenido, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ExtractionException("No se pudo leer el TXT: " + e.getMessage(), e);
        }
    }
}