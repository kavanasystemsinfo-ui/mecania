package com.kavanamecania.mecania.domain.extraction;

/**
 * Estrategia para extraer texto plano de un archivo.
 * Una implementación por tipo de documento (PDF, TXT, DOCX).
 * La lógica vive en infraestructura/extraction; esta interfaz es del dominio.
 */
public interface TextExtractor {

    /**
     * Tipos de documento que este extractor sabe manejar.
     * Ej: ["PDF"]
     */
    boolean soporta(DocumentoTipo tipo);

    /**
     * Extrae el texto del archivo. Implementaciones deben ser idempotentes y thread-safe.
     *
     * @param contenido bytes del archivo
     * @return texto plano extraído
     * @throws ExtractionException si el archivo no se puede parsear
     */
    String extraer(byte[] contenido) throws ExtractionException;
}