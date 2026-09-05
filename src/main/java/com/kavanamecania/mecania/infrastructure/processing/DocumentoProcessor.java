package com.kavanamecania.mecania.infrastructure.processing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Orquestador asíncrono del procesamiento de documentos.
 *
 * <p>Flujo: escucha el disparo (vía {@link DocumentoSubidoEventListener} tras
 * el commit de la subida), ejecuta el paso transaccional en
 * {@link DocumentoProcesadorTransaccional} y actualiza el estado del documento
 * (LISTO o ERROR).</p>
 *
 * <p>El método {@link #procesar(Long)} corre en el pool
 * "documentoProcessorExecutor" (ver {@link com.kavanamecania.mecania.config.AsyncConfig}).</p>
 *
 * <p>Esta clase NO inyecta {@link com.kavanamecania.mecania.application.DocumentoService}
 * para evitar ciclos. La actualización de estado vive en
 * {@link DocumentoProcessorStatusUpdater}.</p>
 */
@Component
public class DocumentoProcessor {

    private static final Logger log = LoggerFactory.getLogger(DocumentoProcessor.class);

    private final DocumentoProcesadorTransaccional procesadorTransaccional;
    private final DocumentoProcessorStatusUpdater statusUpdater;

    public DocumentoProcessor(
            DocumentoProcesadorTransaccional procesadorTransaccional,
            DocumentoProcessorStatusUpdater statusUpdater) {
        this.procesadorTransaccional = procesadorTransaccional;
        this.statusUpdater = statusUpdater;
    }

    /**
     * Procesa un documento de forma asíncrona.
     *
     * @param documentoId ID del documento a procesar
     */
    @Async("documentoProcessorExecutor")
    public void procesar(Long documentoId) {
        log.info("Iniciando procesamiento asíncrono del documento {}", documentoId);
        try {
            doProcesar(documentoId);
            statusUpdater.marcarListo(documentoId);
            log.info("Documento {} procesado correctamente", documentoId);
        } catch (Exception e) {
            log.warn("Error procesando documento {}: {}", documentoId, e.getMessage(), e);
            statusUpdater.marcarError(documentoId, e.getMessage());
        }
    }

    /**
     * Lógica de procesamiento delegada al bean transaccional, expuesta para
     * testearla sin @Async. La transacción SÍ se aplica porque se invoca a un
     * bean distinto (no es self-invocation).
     */
    public void doProcesar(Long documentoId) throws Exception {
        procesadorTransaccional.doProcesar(documentoId);
    }
}