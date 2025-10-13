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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "solicitudes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Solicitudes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "soli_id")
    private Long soliId;

    @Column(name = "soli_descri", columnDefinition = "TEXT")
    private String soliDescri;

    @Column(name = "soli_motivo", columnDefinition = "TEXT")
    private String soliMotivo;

    // Relación con SoliEstados
    @ManyToOne
    @JoinColumn(name = "soli_estados_soli_estado_id", nullable = false)
    private SoliEstados soliEstado;

    // Relación con SoliTipos
    @ManyToOne
    @JoinColumn(name = "soli_tipos_soli_tipo_id", nullable = false)
    private SoliTipos soliTipo;

    // Relación con Usuario (quien solicita)
    @ManyToOne
    @JoinColumn(name = "usuario_us_id", nullable = false)
    private Usuario usuario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Obtiene el nombre del usuario solicitante
     */
    public String getUsuarioNombre() {
        if (usuario != null && usuario.getPersona() != null) {
            return usuario.getPersona().getPerNom1() + " " + usuario.getPersona().getPerApe1();
        }
        return "Sin usuario";
    }

    /**
     * Obtiene el tipo de solicitud
     */
    public String getTipoSolicitud() {
        return soliTipo != null ? soliTipo.getSoliTipoNom() : "Sin tipo";
    }

    /**
     * Obtiene el estado de la solicitud
     */
    public String getEstadoSolicitud() {
        return soliEstado != null ? soliEstado.getSoliEstadoNom() : "Sin estado";
    }

    /**
     * Verifica si la solicitud está pendiente
     */
    public boolean estaPendiente() {
        return soliEstado != null && "Pendiente".equalsIgnoreCase(soliEstado.getSoliEstadoNom());
    }

    /**
     * Verifica si la solicitud está aprobada
     */
    public boolean estaAprobada() {
        return soliEstado != null && "Aprobada".equalsIgnoreCase(soliEstado.getSoliEstadoNom());
    }

    /**
     * Verifica si la solicitud está rechazada
     */
    public boolean estaRechazada() {
        return soliEstado != null && "Rechazada".equalsIgnoreCase(soliEstado.getSoliEstadoNom());
    }

    /**
     * Verifica si la solicitud está completada
     */
    public boolean estaCompletada() {
        return soliEstado != null && "Completada".equalsIgnoreCase(soliEstado.getSoliEstadoNom());
    }

    /**
     * Verifica si tiene descripción
     */
    public boolean tieneDescripcion() {
        return soliDescri != null && !soliDescri.trim().isEmpty();
    }

    /**
     * Verifica si tiene motivo
     */
    public boolean tieneMotivo() {
        return soliMotivo != null && !soliMotivo.trim().isEmpty();
    }

    /**
     * Obtiene descripción corta (para listados)
     */
    public String getDescripcionCorta() {
        if (soliDescri == null || soliDescri.trim().isEmpty()) {
            return "Sin descripción";
        }
        return soliDescri.length() > 80 
            ? soliDescri.substring(0, 77) + "..." 
            : soliDescri;
    }
}