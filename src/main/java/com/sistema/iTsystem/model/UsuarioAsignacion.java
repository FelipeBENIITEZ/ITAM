package com.sistema.iTsystem.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario_asignaciones")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAsignacion {

    @EmbeddedId
    private UsuarioAsignacionId id;

    @Column(name = "asignacion_fecha", nullable = false)
    private LocalDate asignacionFecha = LocalDate.now();

    @Column(name = "asignacion_motivo", length = 255)
    private String asignacionMotivo;

    // Relación con Usuario (usa MapsId para sincronizar con la PK compuesta)
    @ManyToOne
    @MapsId("usuarioId")
    @JoinColumn(name = "usuario_us_id", nullable = false)
    private Usuario usuario;

    // Relación con Activo
    @ManyToOne
    @MapsId("activoId")
    @JoinColumn(name = "activo_activo_id", nullable = false)
    private Activo activo;
}