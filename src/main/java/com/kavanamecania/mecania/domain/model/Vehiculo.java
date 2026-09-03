package com.kavanamecania.mecania.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    @Column(nullable = false)
    private String combustible;

    @Column
    private Long kilometraje;

    @Column
    private String matricula;

    @Column(nullable = false, updatable = false)
    private java.time.Instant createdAt = java.time.Instant.now();
}