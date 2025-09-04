package com.sistema.iTsystem.model;

import java.math.BigDecimal;
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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "contrato_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contrat_id")
    private Long contratId;

    @Column(name = "contrat_nom", nullable = false, length = 100)
    private String contratNom;

    @Column(name = "contrat_descri", length = 255)
    private String contratDescri;

    @Column(name = "contrat_fecha_inicio", nullable = false)
    private LocalDate contratFechaInicio;

    @Column(name = "contrat_fecha_fin")
    private LocalDate contratFechaFin;

    @Column(name = "contrat_monto", precision = 12, scale = 2)
    private BigDecimal contratMonto;

    // Relación con Proveedores
    @ManyToOne
    @JoinColumn(name = "prov_id", nullable = false)
    private Proveedores proveedor;

    @Column(name = "created_at")
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
}