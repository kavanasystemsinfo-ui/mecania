package com.kavanamecania.mecania.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_embeddings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String content; // original text chunk

    @Column(nullable = false, length = 500)
    private String source; // e.g., manual title, section

    // pgvector specific column - we'll use PostgreSQL array of double precision for simplicity
    // In a real setup, you'd use the pgvector type via a custom type or Hibernate Types library
    @Column(name = "embedding", columnDefinition = "vector(384)") // example dimension for all-MiniLM-L6-v2
    private double[] embedding; // Note: This mapping may need additional configuration

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }
}
