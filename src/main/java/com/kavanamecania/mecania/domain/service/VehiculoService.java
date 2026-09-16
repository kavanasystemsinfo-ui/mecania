package com.kavanamecania.mecania.domain.service;

import com.kavanamecania.mecania.application.dto.VehiculoRequest;
import com.kavanamecania.mecania.domain.exception.VehiculoDuplicadoException;
import com.kavanamecania.mecania.domain.exception.VehiculoNotFoundException;
import com.kavanamecania.mecania.domain.model.Vehiculo;
import com.kavanamecania.mecania.infrastructure.repository.VehiculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operaciones sobre vehículos, siempre acotadas al usuario dueño
 * (multi-tenencia: un usuario nunca toca vehículos ajenos).
 */
@Service
public class VehiculoService {

    private final VehiculoRepository repository;

    public VehiculoService(VehiculoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Vehiculo> findByUsuarioId(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public Vehiculo findById(Long id, Long usuarioId) {
        return repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new VehiculoNotFoundException(id));
    }

    @Transactional
    public Vehiculo create(VehiculoRequest req, Long usuarioId) {
        if (repository.existsByUsuarioIdAndMarcaAndModeloAndAnio(
                usuarioId, req.marca(), req.modelo(), req.anio())) {
            throw new VehiculoDuplicadoException(
                    usuarioId, req.marca(), req.modelo(), req.anio());
        }
        Vehiculo v = Vehiculo.builder()
                .usuarioId(usuarioId)
                .marca(req.marca())
                .modelo(req.modelo())
                .anio(req.anio())
                .combustible(req.combustible())
                .kilometraje(req.kilometraje())
                .matricula(req.matricula())
                .build();
        return repository.save(v);
    }

    @Transactional
    public Vehiculo update(Long id, VehiculoRequest req, Long usuarioId) {
        Vehiculo v = findById(id, usuarioId);
        v.setMarca(req.marca());
        v.setModelo(req.modelo());
        v.setAnio(req.anio());
        v.setCombustible(req.combustible());
        v.setKilometraje(req.kilometraje());
        v.setMatricula(req.matricula());
        return repository.save(v);
    }

    @Transactional
    public void delete(Long id, Long usuarioId) {
        if (!repository.existsByIdAndUsuarioId(id, usuarioId)) {
            throw new VehiculoNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
