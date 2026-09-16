package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.application.AlertaService;
import com.kavanamecania.mecania.application.dto.AlertaRequest;
import com.kavanamecania.mecania.application.dto.AlertaResponse;
import com.kavanamecania.mecania.domain.model.Alerta;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Alertas de mantenimiento de un vehículo.
 */
@RestController
@RequestMapping("/api/vehiculos/{vehiculoId}/alertas")
@Validated
public class AlertaController {

    private final AlertaService alertaService;
    private final VehiculoRepository vehiculoRepository;

    public AlertaController(AlertaService alertaService, VehiculoRepository vehiculoRepository) {
        this.alertaService = alertaService;
        this.vehiculoRepository = vehiculoRepository;
    }

    @GetMapping
    public ResponseEntity<List<AlertaResponse>> listar(@NotNull @PathVariable Long vehiculoId) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            return ResponseEntity.notFound().build();
        }
        List<AlertaResponse> alertas = alertaService.listar(vehiculoId).stream()
                .map(AlertaResponse::from)
                .toList();
        return ResponseEntity.ok(alertas);
    }

    @PostMapping
    public ResponseEntity<AlertaResponse> crear(
            @NotNull @PathVariable Long vehiculoId,
            @Valid @RequestBody AlertaRequest request) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            return ResponseEntity.notFound().build();
        }
        Alerta creada = alertaService.crear(vehiculoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AlertaResponse.from(creada));
    }

    @PutMapping("/{alertaId}")
    public ResponseEntity<AlertaResponse> actualizar(
            @NotNull @PathVariable Long vehiculoId,
            @NotNull @PathVariable Long alertaId,
            @Valid @RequestBody AlertaRequest request) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            return ResponseEntity.notFound().build();
        }
        Alerta actualizada = alertaService.actualizar(vehiculoId, alertaId, request);
        return ResponseEntity.ok(AlertaResponse.from(actualizada));
    }

    @DeleteMapping("/{alertaId}")
    public ResponseEntity<Void> eliminar(
            @NotNull @PathVariable Long vehiculoId,
            @NotNull @PathVariable Long alertaId) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            return ResponseEntity.notFound().build();
        }
        alertaService.eliminar(vehiculoId, alertaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vencidas")
    public ResponseEntity<List<AlertaResponse>> vencidas(@NotNull @PathVariable Long vehiculoId) {
        if (!vehiculoRepository.existsById(vehiculoId)) {
            return ResponseEntity.notFound().build();
        }
        List<AlertaResponse> vencidas = alertaService.vencidas(vehiculoId).stream()
                .map(AlertaResponse::from)
                .toList();
        return ResponseEntity.ok(vencidas);
    }
}
