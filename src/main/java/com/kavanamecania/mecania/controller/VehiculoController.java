package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.application.dto.VehiculoRequest;
import com.kavanamecania.mecania.application.dto.VehiculoResponse;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.service.VehiculoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    @GetMapping
    public List<VehiculoResponse> list(@RequestParam(required = false) Long usuarioId) {
        List<Vehiculo> vehiculos = (usuarioId != null)
                ? service.findByUsuarioId(usuarioId)
                : service.findAll();
        return vehiculos.stream().map(VehiculoResponse::from).toList();
    }

    @GetMapping("/{id}")
    public VehiculoResponse get(@PathVariable Long id) {
        return VehiculoResponse.from(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<VehiculoResponse> create(@Valid @RequestBody VehiculoRequest req) {
        Vehiculo creado = service.create(req);
        return ResponseEntity
                .created(URI.create("/api/vehiculos/" + creado.getId()))
                .body(VehiculoResponse.from(creado));
    }

    @PutMapping("/{id}")
    public VehiculoResponse update(@PathVariable Long id, @Valid @RequestBody VehiculoRequest req) {
        return VehiculoResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
