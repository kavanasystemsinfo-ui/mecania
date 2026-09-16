package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.application.BusquedaManualesService;
import com.kavanamecania.mecania.application.ResultadoImportacion;
import com.kavanamecania.mecania.application.dto.CandidatoManualResponse;
import com.kavanamecania.mecania.application.dto.ImportacionResponse;
import com.kavanamecania.mecania.application.dto.ImportarManualRequest;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import com.kavanamecania.mecania.security.SecurityUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Búsqueda asistida de manuales para un vehículo.
 *
 * <p>{@code /candidatos} solo propone enlaces: no descarga nada. La descarga
 * ocurre en {@code /importar}, cuando el usuario acepta una URL concreta.</p>
 */
@RestController
@RequestMapping("/api/vehiculos/{vehiculoId}/manuales")
@Validated
public class BusquedaManualesController {

    private final BusquedaManualesService busquedaManualesService;
    private final VehiculoRepository vehiculoRepository;

    public BusquedaManualesController(BusquedaManualesService busquedaManualesService,
                                      VehiculoRepository vehiculoRepository) {
        this.busquedaManualesService = busquedaManualesService;
        this.vehiculoRepository = vehiculoRepository;
    }

    @GetMapping("/candidatos")
    public ResponseEntity<List<CandidatoManualResponse>> buscarCandidatos(
            @NotNull @PathVariable Long vehiculoId,
            @RequestParam(value = "q", required = false) String consulta) {
        if (!vehiculoRepository.existsByIdAndUsuarioId(vehiculoId, SecurityUtils.usuarioIdActual())) {
            return ResponseEntity.notFound().build();
        }

        List<CandidatoManualResponse> candidatos = busquedaManualesService.buscar(vehiculoId, consulta).stream()
                .map(CandidatoManualResponse::from)
                .toList();
        return ResponseEntity.ok(candidatos);
    }

    @PostMapping("/importar")
    public ResponseEntity<ImportacionResponse> importarManual(
            @NotNull @PathVariable Long vehiculoId,
            @Valid @RequestBody ImportarManualRequest request) {
        if (!vehiculoRepository.existsByIdAndUsuarioId(vehiculoId, SecurityUtils.usuarioIdActual())) {
            return ResponseEntity.notFound().build();
        }

        ResultadoImportacion resultado = busquedaManualesService.importar(vehiculoId, request.url());
        HttpStatus estado = resultado.yaExistia() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(estado).body(ImportacionResponse.from(resultado));
    }
}
