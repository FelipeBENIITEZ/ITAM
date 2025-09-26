package com.sistema.iTsystem.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "hardware_costos")
public class HardwareCostos {
    
    @Id
    @Column(name = "hw_id")
    private Long hwId;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hw_id")
    @MapsId
    private HardwareInfo hardwareInfo;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pres_id", nullable = false)
    private PresupuestoAreas presupuesto;
    
    @Column(name = "hw_valor_compra", nullable = false, precision = 12, scale = 2)
    private BigDecimal hwValorCompra;
    
    @Column(name = "hw_valor_venta", precision = 12, scale = 2)
    private BigDecimal hwValorVenta = BigDecimal.ZERO;
    
    // Constructores
    public HardwareCostos() {}
    
    public HardwareCostos(HardwareInfo hardwareInfo, PresupuestoAreas presupuesto, BigDecimal hwValorCompra) {
        this.hardwareInfo = hardwareInfo;
        this.presupuesto = presupuesto;
        this.hwValorCompra = hwValorCompra;
        this.hwValorVenta = BigDecimal.ZERO;
    }
    
    // Getters y Setters
    public Long getHwId() {
        return hwId;
    }
    
    public void setHwId(Long hwId) {
        this.hwId = hwId;
    }
    
    public HardwareInfo getHardwareInfo() {
        return hardwareInfo;
    }
    
    public void setHardwareInfo(HardwareInfo hardwareInfo) {
        this.hardwareInfo = hardwareInfo;
    }
    
    public PresupuestoAreas getPresupuesto() {
        return presupuesto;
    }
    
    public void setPresupuesto(PresupuestoAreas presupuesto) {
        this.presupuesto = presupuesto;
    }
    
    public BigDecimal getHwValorCompra() {
        return hwValorCompra;
    }
    
    public void setHwValorCompra(BigDecimal hwValorCompra) {
        this.hwValorCompra = hwValorCompra;
    }
    
    public BigDecimal getHwValorVenta() {
        return hwValorVenta;
    }
    
    public void setHwValorVenta(BigDecimal hwValorVenta) {
        this.hwValorVenta = hwValorVenta;
    }
    
    // Metodo util para calcular perdida/ganancia si fue vendido
    public BigDecimal calcularDiferencia() {
        if (hwValorVenta == null || hwValorVenta.equals(BigDecimal.ZERO)) {
            return BigDecimal.ZERO; // No vendido aún
        }
        return hwValorVenta.subtract(hwValorCompra);
    }
    
    // Metodo para verificar si fue vendido
    public boolean isVendido() {
        return hwValorVenta != null && hwValorVenta.compareTo(BigDecimal.ZERO) > 0;
    }
}
