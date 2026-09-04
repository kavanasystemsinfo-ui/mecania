package com.kavanamecania.mecania.infrastructure.repository;

import com.kavanamecania.mecania.domain.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {
    List<Documento> findByVehiculoId(Long vehiculoId);
    Optional<Documento> findByIdAndVehiculoId(Long id, Long vehiculoId);
}