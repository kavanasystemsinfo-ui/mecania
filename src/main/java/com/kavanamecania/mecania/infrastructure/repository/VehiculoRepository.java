package com.kavanamecania.mecania.infrastructure.repository;

import com.kavanamecania.mecania.domain.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    List<Vehiculo> findByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndMarcaAndModeloAndAnio(
            Long usuarioId, String marca, String modelo, Integer anio);

    Optional<Vehiculo> findByIdAndUsuarioId(Long id, Long usuarioId);

    boolean existsByIdAndUsuarioId(Long id, Long usuarioId);
}
