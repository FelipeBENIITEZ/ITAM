package com.sistema.iTsystem.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contrato_activo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoActivo {

    @EmbeddedId
    private ContratoActivoId id;

    @ManyToOne
    @MapsId("activoId")
    @JoinColumn(name = "activo_id")
    private Activo activo;

    @ManyToOne
    @MapsId("contratId")
    @JoinColumn(name = "contrat_id")
    private ContratoInfo contratoInfo;

    @Column(name = "fecha_vinculacion")
    private LocalDate fechaVinculacion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (fechaVinculacion == null) {
            fechaVinculacion = LocalDate.now();
        }
    }

    // ==================== CLASE INTERNA: ID COMPUESTO ====================

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ContratoActivoId implements Serializable {
        
        private static final long serialVersionUID = 1L;

        @Column(name = "activo_id")
        private Long activoId;

        @Column(name = "contrat_id")
        private Long contratId;
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Obtiene el nombre del activo vinculado
     */
    public String getActivoNombre() {
        return activo != null ? activo.getActivoNom() : "Sin activo";
    }

    /**
     * Obtiene el número del contrato vinculado
     */
    public String getContratoNumero() {
        return contratoInfo != null ? contratoInfo.getContratNumero() : "Sin contrato";
    }

    /**
     * Obtiene el proveedor del contrato
     */
    public String getProveedorNombre() {
        return contratoInfo != null ? contratoInfo.getProveedorNombre() : "Sin proveedor";
    }
}