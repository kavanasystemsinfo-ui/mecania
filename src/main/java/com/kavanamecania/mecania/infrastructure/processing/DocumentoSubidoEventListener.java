package com.kavanamecania.mecania.infrastructure.processing;

import com.kavanamecania.mecania.application.evento.DocumentoSubidoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Escucha el evento de documento subido y dispara el procesamiento asíncrono
 * SOLO después de que la transacción del {@code DocumentoService} haya hecho
 * commit. Con {@code AFTER_COMMIT} se garantiza que el hilo async lee el
 * documento ya persistido (evita la carrera @Async dentro de @Transactional).
 */
@Component
public class DocumentoSubidoEventListener {

    private static final Logger log = LoggerFactory.getLogger(DocumentoSubidoEventListener.class);

    private final DocumentoProcessor documentoProcessor;

    public DocumentoSubidoEventListener(DocumentoProcessor documentoProcessor) {
        this.documentoProcessor = documentoProcessor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentoSubido(DocumentoSubidoEvent evento) {
        log.info("Documento {} commiteado, disparando procesamiento async", evento.documentoId());
        documentoProcessor.procesar(evento.documentoId());
    }
}