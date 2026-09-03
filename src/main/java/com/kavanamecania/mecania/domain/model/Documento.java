package com.kavanamecania.mecania.domain.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(nullable = false)
    private Long tamanoBytes;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoProcesamiento estado;

    @Column
    private String mensajeError;

    @Column(nullable = false, updatable = false)
    private java.time.Instant createdAt = java.time.Instant.now();

    public enum TipoDocumento {
        PDF, TXT, DOCX
    }

    public enum EstadoProcesamiento {
        PROCESANDO, LISTO, ERROR
    }
}