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
@Table(name = "activo_historial_estados")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivoHistorialEstados {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Long historialId;

    // Relación con Activo
    @ManyToOne
    @JoinColumn(name = "activo_id", nullable = false)
    private Activo activo;

    // Estado anterior (puede ser null en el primer registro)
    @ManyToOne
    @JoinColumn(name = "estado_anterior_id")
    private EstadoActivo estadoAnterior;

    // Estado nuevo
    @ManyToOne
    @JoinColumn(name = "estado_nuevo_id", nullable = false)
    private EstadoActivo estadoNuevo;

    // Usuario que realizó el cambio
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @PrePersist
    protected void onCreate() {
        fechaCambio = LocalDateTime.now();
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Obtiene el nombre del activo
     */
    public String getActivoNombre() {
        return activo != null ? activo.getActivoNom() : "Sin activo";
    }

    /**
     * Obtiene el nombre del estado anterior
     */
    public String getEstadoAnteriorNombre() {
        return estadoAnterior != null ? estadoAnterior.getEstadoNom() : "Estado inicial";
    }

    /**
     * Obtiene el nombre del estado nuevo
     */
    public String getEstadoNuevoNombre() {
        return estadoNuevo != null ? estadoNuevo.getEstadoNom() : "Sin estado";
    }

    /**
     * Obtiene el nombre del usuario que realizó el cambio
     */
    public String getUsuarioNombre() {
        if (usuario != null && usuario.getPersona() != null) {
            return usuario.getPersona().getPerNom1() + " " + usuario.getPersona().getPerApe1();
        }
        return "Sin usuario";
    }

    /**
     * Verifica si tiene motivo
     */
    public boolean tieneMotivo() {
        return motivo != null && !motivo.trim().isEmpty();
    }

    /**
     * Verifica si tiene observaciones
     */
    public boolean tieneObservaciones() {
        return observaciones != null && !observaciones.trim().isEmpty();
    }

    /**
     * Obtiene la descripción del cambio en formato legible
     */
    public String getDescripcionCambio() {
        return String.format("Cambio de '%s' a '%s' por %s el %s",
            getEstadoAnteriorNombre(),
            getEstadoNuevoNombre(),
            getUsuarioNombre(),
            fechaCambio.toString()
        );
    }

    /**
     * Verifica si es el registro inicial (sin estado anterior)
     */
    public boolean esRegistroInicial() {
        return estadoAnterior == null;
    }
}