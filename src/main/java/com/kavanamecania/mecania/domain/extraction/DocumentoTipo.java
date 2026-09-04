package com.kavanamecania.mecania.domain.extraction;

/**
 * Tipo de archivo que sabemos procesar.
 * Se mantiene en domain porque la lógica de negocio (qué tipos aceptamos) es de dominio.
 */
public enum DocumentoTipo {
    PDF, TXT, DOCX;

    public static DocumentoTipo desdeExtension(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename is null");
        }
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) {
            throw new IllegalArgumentException("No extension found in filename: " + filename);
        }
        String ext = filename.substring(idx + 1).toUpperCase();
        try {
            return DocumentoTipo.valueOf(ext);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported extension: " + ext);
        }
    }
}