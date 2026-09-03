package com.kavanamecania.mecania.controller;

import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.domain.service.VehiculoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    @Autowired
    private VehiculoService vehiculoService;

    @GetMapping
    public List<Vehiculo> getAllVehiculos() {
        return vehiculoService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehiculo> getVehiculoById(@PathVariable Long id) {
        return vehiculoService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Vehiculo createVehiculo(@RequestBody Vehiculo vehiculo) {
        return vehiculoService.save(vehiculo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehiculo> updateVehiculo(@PathVariable Long id, @RequestBody Vehiculo vehiculoDetails) {
        return vehiculoService.findById(id)
                .map(vehiculo -> {
                    vehiculo.setMarca(vehiculoDetails.getMarca());
                    vehiculo.setModelo(vehiculoDetails.getModelo());
                    vehiculo.setAnio(vehiculoDetails.getAnio());
                    vehiculo.setCombustible(vehiculoDetails.getCombustible());
                    vehiculo.setKilometraje(vehiculoDetails.getKilometraje());
                    vehiculo.setMatricula(vehiculoDetails.getMatricula());
                    Vehiculo updatedVehiculo = vehiculoService.save(vehiculo);
                    return ResponseEntity.ok(updatedVehiculo);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehiculo(@PathVariable Long id) {
        return vehiculoService.findById(id)
                .map(vehiculo -> {
                    vehiculoService.deleteById(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}