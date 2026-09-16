package com.kavanamecania.mecania.infrastructure.repository;

import com.kavanamecania.mecania.domain.model.Alerta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByVehiculoId(Long vehiculoId);
    Optional<Alerta> findByVehiculoIdAndId(Long vehiculoId, Long id);
}
