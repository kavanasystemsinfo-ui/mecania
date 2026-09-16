package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.application.dto.VehiculoRequest;
import com.kavanamecania.mecania.application.dto.VehiculoResponse;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.service.VehiculoService;
import com.kavanamecania.mecania.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * CRUD de vehículos del usuario autenticado. El propietario sale del token,
 * no del body: un usuario solo ve y toca sus vehículos.
 */
@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehiculoResponse> list() {
        Long usuarioId = SecurityUtils.usuarioIdActual();
        return service.findByUsuarioId(usuarioId).stream().map(VehiculoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VehiculoResponse get(@PathVariable Long id) {
        return VehiculoResponse.from(service.findById(id, SecurityUtils.usuarioIdActual()));
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> create(@Valid @RequestBody VehiculoRequest req) {
        Vehiculo creado = service.create(req, SecurityUtils.usuarioIdActual());
        return ResponseEntity
                .created(URI.create("/api/vehiculos/" + creado.getId()))
                .body(VehiculoResponse.from(creado));
    }

    @PutMapping("/{id}")
    public VehiculoResponse update(@PathVariable Long id, @Valid @RequestBody VehiculoRequest req) {
        return VehiculoResponse.from(service.update(id, req, SecurityUtils.usuarioIdActual()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id, SecurityUtils.usuarioIdActual());
    }
}
