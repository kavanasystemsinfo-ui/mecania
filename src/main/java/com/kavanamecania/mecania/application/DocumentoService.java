package com.kavanamecania.mecania.application;

import com.kavanamecania.mecania.application.evento.DocumentoSubidoEvent;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.storage.AlmacenamientoArchivos;
import com.kavanamecania.mecania.infrastructure.repository.DocumentoRepository;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final VehiculoRepository vehiculoRepository;
    private final AlmacenamientoArchivos almacenamientoArchivos;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentoService(DocumentoRepository documentoRepository,
                            VehiculoRepository vehiculoRepository,
                            AlmacenamientoArchivos almacenamientoArchivos,
                            ApplicationEventPublisher eventPublisher) {
        this.documentoRepository = documentoRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.almacenamientoArchivos = almacenamientoArchivos;
        this.eventPublisher = eventPublisher;
    }

    public AlmacenamientoArchivos getAlmacenamientoArchivos() {
        return almacenamientoArchivos;
    }

    @Transactional(readOnly = true)
    public List<Documento> findByVehiculoId(Long vehiculoId) {
        return documentoRepository.findByVehiculoId(vehiculoId);
    }

    @Transactional(readOnly = true)
    public Optional<Documento> findByIdAndVehiculoId(Long id, Long vehiculoId) {
        return documentoRepository.findByIdAndVehiculoId(id, vehiculoId);
    }

    @Transactional
    public Documento subirDocumento(Long vehiculoId, MultipartFile file) {
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Vehiculo no encontrado: " + vehiculoId));

        // Validate file type
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File must have a name");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toUpperCase();
        Documento.TipoDocumento tipo;
        try {
            tipo = Documento.TipoDocumento.valueOf(extension);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de archivo no soportado: " + extension +
                    ". Soportados: PDF, TXT, DOCX");
        }

        // Validate size (10MB limit)
        long maxSizeBytes = 10 * 1024 * 1024; // 10MB
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Archivo demasiado grande: " + file.getSize() +
                    " bytes. Máximo permitido: " + maxSizeBytes + " bytes");
        }

        try {
            String relativePath = almacenamientoArchivos.guardarArchivo(file, "vehiculos/" + vehiculoId + "/documentos");

            Documento documento = Documento.builder()
                    .vehiculo(vehiculo)
                    .nombre(filename)
                    .tipo(tipo)
                    .rutaAlmacenamiento(relativePath)
                    .estado(Documento.EstadoProcesamiento.PROCESANDO)
                    .tamanoBytes(file.getSize())
                    .mensajeError(null)
                    .build(); // createdAt set by @PrePersist

            Documento guardado = documentoRepository.save(documento);

            // Publicar evento: un listener AFTER_COMMIT dispara el procesamiento
            // async SOLO cuando la transacción ya ha commiteado. Evita la carrera
            // de lanzar @Async dentro de la transacción (ver ADR 004).
            eventPublisher.publishEvent(new DocumentoSubidoEvent(guardado.getId()));

            return guardado;
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el archivo: " + e.getMessage(), e);
        }
    }

    /**
     * Reads the file bytes for a given document (used by controller for download).
     *
     * @param documentoId ID of the document
     * @param vehiculoId ID of the vehicle to verify ownership
     * @return file contents as byte array
     * @throws Exception if file cannot be read
     */
    public byte[] obtenerArchivoBytes(Long documentoId, Long vehiculoId) throws Exception {
        Documento documento = documentoRepository.findByIdAndVehiculoId(documentoId, vehiculoId)
                .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado o no pertenece al vehículo"));
        return almacenamientoArchivos.leerArchivo(documento.getRutaAlmacenamiento());
    }
}