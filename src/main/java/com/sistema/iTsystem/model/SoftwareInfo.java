package com.sistema.iTsystem.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "software_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoftwareInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sft_id")
    private Long sftId;

    @Column(name = "sft_nom", length = 150, nullable = false)
    private String sftNom;

    @Column(name = "sft_version", length = 50)
    private String sftVersion;

    // Relación uno a uno con Activo
    @OneToOne
    @JoinColumn(name = "activo_activo_id", nullable = false, unique = true)
    private Activo activo;

    // Relación con Proveedor (opcional)
    @ManyToOne
    @JoinColumn(name = "proveedores_prov_id")
    private Proveedores proveedor;

    // Relación con SoftTipo
    @ManyToOne
    @JoinColumn(name = "soft_tipo_soft_tipo_id", nullable = false)
    private SoftTipo softTipo;

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

    // Método helper
    public String getNombreCompleto() {
        return sftVersion != null ? sftNom + " " + sftVersion : sftNom;
    }
}