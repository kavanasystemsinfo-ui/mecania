package com.kavanamecania.mecania.infrastructure.repository;

import com.kavanamecania.mecania.domain.model.DocumentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentEmbeddingRepository extends JpaRepository<DocumentEmbedding, Long> {
    // Add custom methods for vector similarity search if needed
    // For example, using @Query with native SQL for pgvector
}
