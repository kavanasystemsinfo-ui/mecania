package com.kavanamecania.mecania.infrastructure.processing;

import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.infrastructure.repository.DocumentoRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsula las actualizaciones de estado de Documento desde el procesador async.
 * Se separa de DocumentoService para evitar ciclos de dependencias
 * (DocumentoProcessor necesita escribir estado, pero DocumentoService no debe
 * depender del processor para mantener cohesión).
 */
@Component
public class DocumentoProcessorStatusUpdater {

    private final DocumentoRepository documentoRepository;

    public DocumentoProcessorStatusUpdater(DocumentoRepository documentoRepository) {
        this.documentoRepository = documentoRepository;
    }

    @Transactional(readOnly = true)
    public Documento findDocumento(Long documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalStateException("Documento no encontrado: " + documentoId));
    }

    @Transactional
    public void marcarListo(Long documentoId) {
        documentoRepository.findById(documentoId).ifPresent(d -> {
            d.setEstado(Documento.EstadoProcesamiento.LISTO);
            d.setMensajeError(null);
            documentoRepository.save(d);
        });
    }

    @Transactional
    public void marcarError(Long documentoId, String mensaje) {
        documentoRepository.findById(documentoId).ifPresent(d -> {
            d.setEstado(Documento.EstadoProcesamiento.ERROR);
            d.setMensajeError(mensaje);
            documentoRepository.save(d);
        });
    }
}