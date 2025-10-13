package com.sistema.iTsystem.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

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
@Table(name = "mantenimiento")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mant_id")
    private Long mantId;

    @Column(name = "mant_fecha_ini", nullable = false)
    private LocalDate mantFechaIni;

    @Column(name = "mant_fecha_fin")
    private LocalDate mantFechaFin;

    @Column(name = "mant_descri", columnDefinition = "TEXT")
    private String mantDescri;

    @Column(name = "mant_costo", precision = 12, scale = 2)
    private BigDecimal mantCosto;

    // Relación con HardwareInfo
    @ManyToOne
    @JoinColumn(name = "hardware_info_hw_id", nullable = false)
    private HardwareInfo hardwareInfo;

    // Relación con MantenimientoTipo
    @ManyToOne
    @JoinColumn(name = "mantenimiento_tipo_mant_tipo_id", nullable = false)
    private MantenimientoTipo mantenimientoTipo;

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
     * Verifica si el mantenimiento está en curso
     */
    public boolean estaEnCurso() {
        LocalDate hoy = LocalDate.now();
        return mantFechaFin == null || !hoy.isAfter(mantFechaFin);
    }

    /**
     * Verifica si el mantenimiento está finalizado
     */
    public boolean estaFinalizado() {
        return mantFechaFin != null && LocalDate.now().isAfter(mantFechaFin);
    }

    /**
     * Calcula la duración del mantenimiento en días
     */
    public long getDuracionDias() {
        if (mantFechaFin == null) {
            return ChronoUnit.DAYS.between(mantFechaIni, LocalDate.now());
        }
        return ChronoUnit.DAYS.between(mantFechaIni, mantFechaFin);
    }

    /**
     * Obtiene el costo formateado
     */
    public String getCostoFormateado() {
        if (mantCosto == null) {
            return "Sin costo especificado";
        }
        return String.format("$%.2f", mantCosto);
    }

    /**
     * Obtiene el serial del hardware en mantenimiento
     */
    public String getHardwareSerial() {
        return hardwareInfo != null ? hardwareInfo.getHwSerialNum() : "Sin hardware";
    }

    /**
     * Obtiene el tipo de mantenimiento
     */
    public String getTipoMantenimiento() {
        return mantenimientoTipo != null ? mantenimientoTipo.getMantTipoNom() : "Sin tipo";
    }

    /**
     * Obtiene el periodo del mantenimiento formateado
     */
    public String getPeriodoMantenimiento() {
        if (mantFechaFin == null) {
            return "Desde " + mantFechaIni + " (en curso)";
        }
        return mantFechaIni + " al " + mantFechaFin;
    }

    /**
     * Verifica si tiene descripción
     */
    public boolean tieneDescripcion() {
        return mantDescri != null && !mantDescri.trim().isEmpty();
    }
}