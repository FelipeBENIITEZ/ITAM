package com.sistema.iTsystem.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario_asignaciones")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asignacion_id")
    @EqualsAndHashCode.Include
    private Long asignacionId;

    @ManyToOne
    @JoinColumn(name = "usu_id", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    @ManyToOne
    @JoinColumn(name = "usu_ejecutor_id")
    private Usuario usuarioEjecutor;

    @ManyToOne
    @JoinColumn(name = "solicitud_id")
    private Solicitudes solicitud;

    @Column(name = "asignacion_fecha", nullable = false)
    private LocalDate asignacionFecha = LocalDate.now();

    @Column(name = "devolucion_fecha")
    private LocalDate devolucionFecha;

    @Column(name = "asignacion_motivo", length = 255)
    private String asignacionMotivo;

    @Column(name = "asignacion_observacion", columnDefinition = "TEXT")
    private String asignacionObservacion;

    @Column(name = "asignacion_activa", nullable = false)
    private Boolean asignacionActiva = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (asignacionFecha == null) {
            asignacionFecha = LocalDate.now();
        }
        if (asignacionActiva == null) {
            asignacionActiva = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
