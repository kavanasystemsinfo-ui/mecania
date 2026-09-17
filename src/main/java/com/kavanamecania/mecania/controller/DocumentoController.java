package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.application.DocumentoService;
import com.kavanamecania.mecania.application.dto.DocumentoResponse;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import com.kavanamecania.mecania.security.SecurityUtils;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehiculos/{vehiculoId}/documentos")
@Validated
public class DocumentoController {

    private static final Logger log = LoggerFactory.getLogger(DocumentoController.class);

    private final DocumentoService documentoService;
    private final VehiculoRepository vehiculoRepository;

    public DocumentoController(DocumentoService documentoService, VehiculoRepository vehiculoRepository) {
        this.documentoService = documentoService;
        this.vehiculoRepository = vehiculoRepository;
    }

    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<DocumentoResponse> uploadDocumento(
            @NotNull @PathVariable Long vehiculoId,
            @RequestParam("file") MultipartFile file) {
        // Validate vehicle exists
        if (!vehiculoRepository.existsByIdAndUsuarioId(vehiculoId, SecurityUtils.usuarioIdActual())) {
            return ResponseEntity.notFound().build();
        }

        Documento documento = documentoService.subirDocumento(vehiculoId, file);
        DocumentoResponse response = DocumentoResponse.from(documento);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> listDocumentos(
            @NotNull @PathVariable Long vehiculoId) {
        if (!vehiculoRepository.existsByIdAndUsuarioId(vehiculoId, SecurityUtils.usuarioIdActual())) {
            return ResponseEntity.notFound().build();
        }

        List<Documento> documentos = documentoService.findByVehiculoId(vehiculoId);
        List<DocumentoResponse> response = documentos.stream()
                .map(doc -> DocumentoResponse.from(doc))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentoId}/download")
    public ResponseEntity<Resource> downloadDocumento(
            @NotNull @PathVariable Long vehiculoId,
            @NotNull @PathVariable Long documentoId) {
        // Igual que en la subida y el listado: el vehículo tiene que ser del
        // usuario autenticado. Sin esta comprobación bastaba con adivinar ids
        // ajenos para descargar manuales de otra cuenta.
        if (!vehiculoRepository.existsByIdAndUsuarioId(vehiculoId, SecurityUtils.usuarioIdActual())) {
            return ResponseEntity.notFound().build();
        }

        var documentoOpt = documentoService.findByIdAndVehiculoId(documentoId, vehiculoId);
        if (documentoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var documento = documentoOpt.get();

        try {
            // En flujo: un manual de 25 MB no tiene por qué pasar entero por heap
            // (en producción el contenedor tiene 384 MB y el almacén es remoto).
            InputStream datos = documentoService.getAlmacenamientoArchivos()
                    .leerArchivoStream(documento.getRutaAlmacenamiento());
            String filename = documento.getNombre();

            ResponseEntity.BodyBuilder respuesta = ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            if (documento.getTamanoBytes() != null) {
                respuesta.contentLength(documento.getTamanoBytes());
            }
            return respuesta.body(new InputStreamResource(datos));
        } catch (Exception e) {
            // El detalle va al log del servidor; al cliente no se le enseña la
            // ruta interna del almacén ni el mensaje del proveedor.
            log.error("No se pudo descargar el documento {} del vehículo {}", documentoId, vehiculoId, e);
            return ResponseEntity.status(500).build();
        }
    }
}