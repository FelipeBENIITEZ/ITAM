package com.sistema.iTsystem.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "solicitud_historial_estados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudHistorialEstados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Long historialId;

    @ManyToOne
    @JoinColumn(name = "soli_id", nullable = false)
    private Solicitudes solicitud;

    @ManyToOne
    @JoinColumn(name = "estado_anterior_id")
    private SoliEstados estadoAnterior;

    @ManyToOne
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private SoliEstados estadoNuevo;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        if (fechaCambio == null) {
            fechaCambio = LocalDateTime.now();
        }
    }

    public String getEstadoAnteriorNombre() {
        return estadoAnterior != null ? estadoAnterior.getSoliEstadoNom() : "Creación";
    }

    public String getEstadoNuevoNombre() {
        return estadoNuevo != null ? estadoNuevo.getSoliEstadoNom() : "Sin estado";
    }

    public String getUsuarioNombre() {
        if (usuario != null && usuario.getPersona() != null) {
            return usuario.getPersona().getPerNom1() + " " + usuario.getPersona().getPerApe1();
        }
        return usuario != null ? usuario.getUsuLogin() : "Sin usuario";
    }

    public String getResumenTransicion() {
        return getEstadoAnteriorNombre() + " -> " + getEstadoNuevoNombre();
    }
}
