package com.sistema.iTsystem.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "garantias")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Garantia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "garant_id")
    private Long garantId;

   @Column(name = "garant_fecha_inicio", nullable = false)
    private LocalDate garantFechaInicio;

    @Column(name = "garant_fecha_fin", nullable = false)
    private LocalDate garantFechaFin;

    @Column(name = "garant_vigencia", length = 50)
    private String garantVigencia;

    // Relacion con HardwareInfo (agregare despues)
    @OneToOne
    @JoinColumn(name = "hardware_info_hw_id")
    private HardwareInfo hardwareInfo;

     @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calcularVigencia();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calcularVigencia();
    }


    //Caluculo de la vigencia de la garantia con valores automaticos
    public void calcularVigencia() {
        LocalDate hoy = LocalDate.now();
    
        if (hoy.isAfter(garantFechaFin)) {
            garantVigencia = "Expirada";
        } else {
            long diasRestantes = ChronoUnit.DAYS.between(hoy, garantFechaFin);
        
            if (diasRestantes <= 30) {
                garantVigencia = "Próxima a vencer";
            } else {
                garantVigencia = "Activa";
            }
        }
    }
     public long getDiasRestantes() {
    LocalDate hoy = LocalDate.now();
    if (hoy.isAfter(garantFechaFin)) {
        return 0;
    }
    return ChronoUnit.DAYS.between(hoy, garantFechaFin);
}

/**
 * Obtiene la duración total de la garantía en meses
 */
public long getDuracionEnMeses() {
    return ChronoUnit.MONTHS.between(garantFechaInicio, garantFechaFin);
}
    //Verifica si la garantia esta vigente (dentro del rango de fechas)
     
    public boolean isVigente() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(garantFechaInicio) && !hoy.isAfter(garantFechaFin);
    }
    
}