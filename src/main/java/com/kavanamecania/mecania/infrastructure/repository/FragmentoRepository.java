package com.kavanamecania.mecania.infrastructure.repository;

import com.kavanamecania.mecania.domain.model.Fragmento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FragmentoRepository extends JpaRepository<Fragmento, Long> {
    List<Fragmento> findByDocumentoId(Long documentoId);
}