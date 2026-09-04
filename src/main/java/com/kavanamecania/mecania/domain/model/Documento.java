package com.kavanamecania.mecania.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "documentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoDocumento tipo;

    /**
     * Path relative to storage base directory (returned by AlmacenamientoArchivos.guardarArchivo)
     */
    @Column(nullable = false)
    private String rutaAlmacenamiento;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoProcesamiento estado;

    /**
     * Size in bytes for metadata
     */
    @Column(nullable = false)
    private Long tamanoBytes;

    @Column
    private String mensajeError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * For future RAG phase: vector embedding of processed text chunks
     * Stored as double array for now; will be migrated to pgvector vector type later.
     */
    private double[] embedding; // Will be populated in phase 2

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public enum TipoDocumento {
        PDF, TXT, DOCX
    }

    public enum EstadoProcesamiento {
        PROCESANDO, LISTO, ERROR
    }
}