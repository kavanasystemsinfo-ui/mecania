package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.application.DocumentoService;
import com.kavanamecania.mecania.application.dto.DocumentoResponse;
import com.kavanamecania.mecania.domain.model.Documento;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vehiculos/{vehiculoId}/documentos")
@Validated
public class DocumentoController {

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
        if (!vehiculoRepository.existsById(vehiculoId)) {
            return ResponseEntity.notFound().build();
        }

        Documento documento = documentoService.subirDocumento(vehiculoId, file);
        DocumentoResponse response = DocumentoResponse.from(documento);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentoResponse>> listDocumentos(
            @NotNull @PathVariable Long vehiculoId) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
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
        var documentoOpt = documentoService.findByIdAndVehiculoId(documentoId, vehiculoId);
        if (documentoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var documento = documentoOpt.get();

        try {
            byte[] data = documentoService.getAlmacenamientoArchivos().leerArchivo(documento.getRutaAlmacenamiento());
            String filename = documento.getNombre();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(new ByteArrayResource(data));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    // For testing purposes, we expose a method to mark as ready/error (not part of public API)
    // In a real app, this would be triggered by an async processing step.
    @PostMapping("/{documentoId}/marcar-listo")
    public ResponseEntity<Void> marcarListo(
            @PathVariable Long vehiculoId,
            @PathVariable Long documentoId) {
        documentoService.marcarComoListo(documentoId, vehiculoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{documentoId}/marcar-error")
    public ResponseEntity<Void> marcarError(
            @PathVariable Long vehiculoId,
            @PathVariable Long documentoId,
            @RequestParam String mensaje) {
        documentoService.marcarComoError(documentoId, vehiculoId, mensaje);
        return ResponseEntity.noContent().build();
    }
}