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
import jakarta.persistence.ManyToOne;
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
    @Column(name = "garan_id")
    private Long garanId;

    @ManyToOne
    @JoinColumn(name = "hw_id", nullable = false)
    private HardwareInfo hardwareInfo;

    @Column(name = "garan_fecha_inicio", nullable = false)
    private LocalDate garanFechaInicio;

    @Column(name = "garan_fecha_fin", nullable = false)
    private LocalDate garanFechaFin;

    @Column(name = "garan_estado", length = 20)
    private String garanEstado;

    @Column(name = "garan_descri", columnDefinition = "TEXT")
    private String garanDescri;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        calcularEstado();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        calcularEstado();
    }

    public void calcularEstado() {
        if (garanFechaFin == null) {
            return;
        }
        LocalDate hoy = LocalDate.now();
        garanEstado = hoy.isAfter(garanFechaFin) ? "Vencida" : "Vigente";
    }

    public long getDiasRestantes() {
        LocalDate hoy = LocalDate.now();
        if (garanFechaFin == null || hoy.isAfter(garanFechaFin)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(hoy, garanFechaFin);
    }

    public long getDuracionEnMeses() {
        if (garanFechaInicio == null || garanFechaFin == null) {
            return 0;
        }
        return ChronoUnit.MONTHS.between(garanFechaInicio, garanFechaFin);
    }

    public boolean isVigente() {
        LocalDate hoy = LocalDate.now();
        return garanFechaFin != null && !hoy.isAfter(garanFechaFin);
    }
}
