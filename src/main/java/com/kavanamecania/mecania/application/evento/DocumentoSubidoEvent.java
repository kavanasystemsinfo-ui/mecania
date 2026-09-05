package com.kavanamecania.mecania.application.evento;

/**
 * Evento de dominio publicado cuando un documento se ha persistido.
 * El procesamiento asíncrono (extracción → chunking → embeddings) se dispara
 * desde un listener AFTER_COMMIT para garantizar que el hilo async vea el
 * documento ya commiteado (ver ADR 004 y verification-gate).
 *
 * @param documentoId ID del documento recién subido
 */
public record DocumentoSubidoEvent(Long documentoId) {
}