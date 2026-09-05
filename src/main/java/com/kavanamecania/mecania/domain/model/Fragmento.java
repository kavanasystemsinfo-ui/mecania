package com.kavanamecania.mecania.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fragmentos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fragmento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false)
    private Documento documento;

    @Column(nullable = false)
    private Integer posicion;

    @Column(nullable = false, length = 8000)
    private String texto;

    // NOTA: El embedding NO se persiste en esta fase.
    // Hibernate-core no soporta el tipo vector de pgvector y nuestra decisión en ADR 004
    // es usar queries nativas en fase 4 con tipo vector(N).
    // Cuando llegue esa migración, este campo se añadirá con un custom Hibernate Type o se
    // sustituirá por una tabla auxiliar `fragmento_embeddings` mapeada con JDBC nativo.
}