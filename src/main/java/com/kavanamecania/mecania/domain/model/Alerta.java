package com.kavanamecania.mecania.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Recordatorio de mantenimiento asociado a un vehículo.
 *
 * <p>Una alerta tiene una {@code fecha} objetivo y una {@link Repetitividad}.
 * Está {@code activa} mientras debe seguir recordándose. La lógica de
 * vencimiento y avance vive en {@code domain/alertas/RevisorAlertas}.</p>
 */
@Entity
@Table(name = "alertas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehiculo_id", nullable = false)
    private Vehiculo vehiculo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoAlerta tipo;

    @Column(nullable = false)
    private String descripcion;

    /** Fecha objetivo del recordatorio (p.ej. día del vencimiento de la ITV). */
    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Repetitividad repetitividad;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /** Vencida = activa y con fecha objetivo igual o anterior a hoy. */
    public boolean estaVencida(LocalDate hoy) {
        return activa && !fecha.isAfter(hoy);
    }

    /**
     * Avanza la fecha un periodo (mes o año). Para {@code UNICA} no hace nada:
     * las alertas únicas se desactivan, no avanzan.
     */
    public void avanzar() {
        switch (repetitividad) {
            case MENSUAL -> fecha = fecha.plusMonths(1);
            case ANUAL -> fecha = fecha.plusYears(1);
            case UNICA -> { /* no avanza: se desactiva en RevisorAlertas */ }
        }
    }
}
