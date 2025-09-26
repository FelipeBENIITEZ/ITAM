package com.sistema.iTsystem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "presupuesto_areas")
public class PresupuestoAreas {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pres_id")
    private Long presId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id", nullable = false)
    private Departamentos departamento;
    
    @Column(name = "pres_asignado", nullable = false, precision = 12, scale = 2)
    private BigDecimal presAsignado;
    
    @Column(name = "pres_usado", precision = 12, scale = 2)
    private BigDecimal presUsado = BigDecimal.ZERO;
    
    @Column(name = "pres_ini_vigencia", nullable = false)
    private LocalDate presIniVigencia;
    
    @Column(name = "pres_fin_vigencia", nullable = false)
    private LocalDate presFinVigencia;
    
    // Constructores
    public PresupuestoAreas() {}
    
    public PresupuestoAreas(Departamentos departamento, BigDecimal presAsignado, 
                           LocalDate presIniVigencia, LocalDate presFinVigencia) {
        this.departamento = departamento;
        this.presAsignado = presAsignado;
        this.presIniVigencia = presIniVigencia;
        this.presFinVigencia = presFinVigencia;
        this.presUsado = BigDecimal.ZERO;
    }
    
    // Getters y Setters
    public Long getPresId() {
        return presId;
    }
    
    public void setPresId(Long presId) {
        this.presId = presId;
    }
    
    public Departamentos getDepartamento() {
        return departamento;
    }
    
    public void setDepartamento(Departamentos departamento) {
        this.departamento = departamento;
    }
    
    public BigDecimal getPresAsignado() {
        return presAsignado;
    }
    
    public void setPresAsignado(BigDecimal presAsignado) {
        this.presAsignado = presAsignado;
    }
    
    public BigDecimal getPresUsado() {
        return presUsado;
    }
    
    public void setPresUsado(BigDecimal presUsado) {
        this.presUsado = presUsado;
    }
    
    public LocalDate getPresIniVigencia() {
        return presIniVigencia;
    }
    
    public void setPresIniVigencia(LocalDate presIniVigencia) {
        this.presIniVigencia = presIniVigencia;
    }
    
    public LocalDate getPresFinVigencia() {
        return presFinVigencia;
    }
    
    public void setPresFinVigencia(LocalDate presFinVigencia) {
        this.presFinVigencia = presFinVigencia;
    }
    
    // Método útil para calcular presupuesto disponible
    public BigDecimal getPresupuestoDisponible() {
        return presAsignado.subtract(presUsado);
    }
    
    // Metodo para verificar si el presupuesto sigue vigente
    public boolean isVigente() {
        LocalDate hoy = LocalDate.now();
        return !hoy.isBefore(presIniVigencia) && !hoy.isAfter(presFinVigencia);
    }
}