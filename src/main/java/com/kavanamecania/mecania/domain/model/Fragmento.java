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
}