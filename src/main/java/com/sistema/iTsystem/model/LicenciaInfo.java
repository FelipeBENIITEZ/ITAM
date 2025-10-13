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
@Table(name = "licencia_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenciaInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "licencia_id")
    private Long licenciaId;

    @Column(name = "licencia_descri", columnDefinition = "TEXT")
    private String licenciaDescri;

    @Column(name = "licencia_ini", nullable = false)
    private LocalDate licenciaIni;

    @Column(name = "licencia_fin")
    private LocalDate licenciaFin;

    @Column(name = "licencia_cupos")
    private Integer licenciaCupos = 1;

    @Column(name = "licencia_costo", precision = 12, scale = 2)
    private BigDecimal licenciaCosto;

    @Column(name = "licencia_usos")
    private Integer licenciaUsos = 0;

    // Relación con LicenciasEstados
    @ManyToOne
    @JoinColumn(name = "licencias_estados_lic_estado_id", nullable = false)
    private LicenciasEstados licenciaEstado;

    // Relación con SoftwareInfo
    @ManyToOne
    @JoinColumn(name = "software_info_sft_id", nullable = false)
    private SoftwareInfo softwareInfo;

    // Relación con LicenciasTipo
    @ManyToOne
    @JoinColumn(name = "licencias_tipo_lic_tipo_id", nullable = false)
    private LicenciasTipo licenciaTipo;

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
     * Verifica si la licencia está vencida
     */
    public boolean estaVencida() {
        return licenciaFin != null && LocalDate.now().isAfter(licenciaFin);
    }

    /**
     * Verifica si la licencia está próxima a vencer (30 días o menos)
     */
    public boolean proximaAVencer() {
        if (licenciaFin == null) {
            return false;
        }
        LocalDate hoy = LocalDate.now();
        return !estaVencida() && ChronoUnit.DAYS.between(hoy, licenciaFin) <= 30;
    }

    /**
     * Calcula días restantes hasta el vencimiento
     */
    public long diasRestantes() {
        if (licenciaFin == null) {
            return -1; // Sin fecha de vencimiento
        }
        LocalDate hoy = LocalDate.now();
        long dias = ChronoUnit.DAYS.between(hoy, licenciaFin);
        return dias >= 0 ? dias : 0;
    }

    /**
     * Verifica si hay cupos disponibles
     */
    public boolean tieneCuposDisponibles() {
        return licenciaCupos != null && licenciaUsos != null && licenciaUsos < licenciaCupos;
    }

    /**
     * Obtiene cupos disponibles
     */
    public int getCuposDisponibles() {
        if (licenciaCupos == null || licenciaUsos == null) {
            return 0;
        }
        return Math.max(0, licenciaCupos - licenciaUsos);
    }

    /**
     * Incrementa el uso de licencias
     */
    public boolean incrementarUso() {
        if (tieneCuposDisponibles()) {
            licenciaUsos++;
            return true;
        }
        return false;
    }

    /**
     * Decrementa el uso de licencias
     */
    public boolean decrementarUso() {
        if (licenciaUsos != null && licenciaUsos > 0) {
            licenciaUsos--;
            return true;
        }
        return false;
    }

    public String getCostoFormateado() {
        if (licenciaCosto == null) {
            return "No especificado";
        }
        return String.format("$%.2f", licenciaCosto);
    }

    /**
     * Obtiene el periodo de vigencia formateado
     */
    public String getPeriodoVigencia() {
        if (licenciaFin == null) {
            return "Desde " + licenciaIni + " (sin vencimiento)";
        }
        return licenciaIni + " al " + licenciaFin;
    }
}