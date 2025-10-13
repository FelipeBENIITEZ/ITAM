package com.sistema.iTsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "hardware_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HardwareInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hw_id")
    private Long hwId;

    @Column(name = "hw_serial_num", length = 100, unique = true, nullable = false)
    private String hwSerialNum;

    @Column(name = "hw_descri")
    private String hwDescri;

    @Column(name = "hw_valor_compra", precision = 12, scale = 2)
    private BigDecimal hwValorCompra;

    // Relación con Activo (uno a uno)
    @OneToOne
    @JoinColumn(name = "activo_activo_id", nullable = false, unique = true)
    private Activo activo;

    // Relación con Modelo
    @ManyToOne
    @JoinColumn(name = "modelo_model_id", nullable = false)
    private Modelo modelo;

    // Relación con Garantía (uno a uno)
    @OneToOne(mappedBy = "hardwareInfo", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private Garantia garantia;
    // Relación con Proveedor
    @ManyToOne
    @JoinColumn(name = "prov_id")
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

    public boolean tieneValorCompra() {
        return hwValorCompra != null && hwValorCompra.compareTo(BigDecimal.ZERO) > 0;
    }

    public String getValorCompraFormateado() {
        if (hwValorCompra == null) {
            return "No especificado";
        }
        return String.format("$%.2f", hwValorCompra);
    }

    public String getProveedorNombre() {
        return proveedor != null ? proveedor.getProvNom() : "Sin proveedor";
    }
}