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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "eventos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Eventos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_fecha")
    private LocalDate eventFecha;

    @Column(name = "event_descri", columnDefinition = "TEXT")
    private String eventDescri;

    @Column(name = "event_impacto", columnDefinition = "TEXT")
    private String eventImpacto;

    // Relación con Activo
    @ManyToOne
    @JoinColumn(name = "activo_activo_id", nullable = false)
    private Activo activo;

    // Relación con EventosNiveles
    @ManyToOne
    @JoinColumn(name = "eventos_niveles_nivel_id", nullable = false)
    private EventosNiveles eventosNivel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (eventFecha == null) {
            eventFecha = LocalDate.now();
        }
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Obtiene el nombre del activo asociado
     */
    public String getActivoNombre() {
        return activo != null ? activo.getActivoNom() : "Sin activo";
    }

    /**
     * Obtiene el nivel del evento
     */
    public String getNivelNombre() {
        return eventosNivel != null ? eventosNivel.getNivelNom() : "Sin nivel";
    }

    /**
     * Verifica si tiene descripción de impacto
     */
    public boolean tieneImpacto() {
        return eventImpacto != null && !eventImpacto.trim().isEmpty();
    }

    /**
     * Obtiene descripción corta (para listados)
     */
    public String getDescripcionCorta() {
        if (eventDescri == null || eventDescri.trim().isEmpty()) {
            return "Sin descripción";
        }
        return eventDescri.length() > 100 
            ? eventDescri.substring(0, 97) + "..." 
            : eventDescri;
    }
}