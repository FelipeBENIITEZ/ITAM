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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "licencia_pagos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenciaPagos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lic_pago_id")
    private Long licPagoId;

    @Column(name = "pago_cost", precision = 12, scale = 2, nullable = false)
    private BigDecimal pagoCost;

    @Column(name = "pago_fecha", nullable = false)
    private LocalDate pagoFecha;

    @Column(name = "pago_descri", columnDefinition = "TEXT")
    private String pagoDescri;

    // Relacion con LicenciaInfo
    @ManyToOne
    @JoinColumn(name = "licencia_info_licencia_id", nullable = false)
    private LicenciaInfo licenciaInfo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (pagoFecha == null) {
            pagoFecha = LocalDate.now();
        }
    }

    // ==================== METODOS HELPER ====================

    /**
     * Obtiene el monto del pago formateado
     */
    public String getMontoFormateado() {
        return String.format("$%.2f", pagoCost);
    }

    /**
     * Verifica si el pago tiene descripción
     */
    public boolean tieneDescripcion() {
        return pagoDescri != null && !pagoDescri.trim().isEmpty();
    }

    /**
     * Obtiene descripcion corta (para listados)
     */
    public String getDescripcionCorta() {
        if (pagoDescri == null || pagoDescri.trim().isEmpty()) {
            return "Sin descripción";
        }
        return pagoDescri.length() > 50 
            ? pagoDescri.substring(0, 47) + "..." 
            : pagoDescri;
    }
}
