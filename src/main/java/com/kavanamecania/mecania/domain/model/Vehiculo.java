package com.kavanamecania.mecania.domain.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "vehiculos",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_vehiculo_usuario_marca_modelo_anio",
                          columnNames = {"usuario_id", "marca", "modelo", "anio"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Combustible combustible;

    @Column
    private Long kilometraje;

    @Column(length = 20)
    private String matricula;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = java.time.Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public Integer getAnio() { return anio; }
    public void setAnio(Integer anio) { this.anio = anio; }
    public Combustible getCombustible() { return combustible; }
    public void setCombustible(Combustible combustible) { this.combustible = combustible; }
    public Long getKilometraje() { return kilometraje; }
    public void setKilometraje(Long kilometraje) { this.kilometraje = kilometraje; }
    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public java.time.Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.Instant createdAt) { this.createdAt = createdAt; }

}
