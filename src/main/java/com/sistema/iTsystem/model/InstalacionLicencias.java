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
@Table(name = "instalacion_licencias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InstalacionLicencias {

    @EmbeddedId
    private InstalacionLicenciasId id;

    @ManyToOne
    @MapsId("hardwareInfoHwId")
    @JoinColumn(name = "hardware_info_hw_id")
    private HardwareInfo hardwareInfo;

    @ManyToOne
    @MapsId("licenciaInfoLicenciaId")
    @JoinColumn(name = "licencia_info_licencia_id")
    private LicenciaInfo licenciaInfo;

    @Column(name = "inst_fecha")
    private LocalDate instFecha;

    @Column(name = "inst_motivo", columnDefinition = "TEXT")
    private String instMotivo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (instFecha == null) {
            instFecha = LocalDate.now();
        }
    }

    // ==================== CLASE INTERNA: ID COMPUESTO ====================

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class InstalacionLicenciasId implements Serializable {
        
        private static final long serialVersionUID = 1L;

        @Column(name = "hardware_info_hw_id")
        private Long hardwareInfoHwId;

        @Column(name = "licencia_info_licencia_id")
        private Long licenciaInfoLicenciaId;
    }

    // ==================== MÉTODOS HELPER ====================

    /**
     * Obtiene el serial del hardware donde está instalada la licencia
     */
    public String getHardwareSerial() {
        return hardwareInfo != null ? hardwareInfo.getHwSerialNum() : "Sin hardware";
    }

    /**
     * Obtiene el nombre del software de la licencia
     */
    public String getSoftwareNombre() {
        return licenciaInfo != null && licenciaInfo.getSoftwareInfo() != null
            ? licenciaInfo.getSoftwareInfo().getSftNom()
            : "Sin software";
    }

    /**
     * Verifica si tiene motivo de instalación
     */
    public boolean tieneMotivo() {
        return instMotivo != null && !instMotivo.trim().isEmpty();
    }
}