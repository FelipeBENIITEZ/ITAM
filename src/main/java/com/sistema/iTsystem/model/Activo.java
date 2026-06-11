package com.sistema.iTsystem.model;

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
@Table(name = "activo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activo_id")
    private Long activoId;

    @Column(name = "activo_codigo", nullable = false, unique = true, length = 50)
    private String activoCodigo;

    @ManyToOne
    @JoinColumn(name = "prov_id")
    private Proveedores proveedor;

    @ManyToOne
    @JoinColumn(name = "cat_id", nullable = false)
    private CategoriasActivo categoria;

    @ManyToOne
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoActivo estado;

    @Column(name = "activo_nom", nullable = false, length = 150)
    private String activoNom;

    @Column(name = "activo_descri", columnDefinition = "TEXT")
    private String activoDescri;

    @Column(name = "activo_fecha_ingreso")
    private LocalDateTime activoFechaIngreso;

    @Column(name = "activo_fecha_egreso")
    private LocalDateTime activoFechaEgreso;

    @Column(name = "activo_activo", nullable = false)
    private Boolean activoActivo = true;

    @OneToOne(mappedBy = "activo", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private HardwareInfo hardwareInfo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (activoFechaIngreso == null) {
            activoFechaIngreso = LocalDateTime.now();
        }
        if (activoActivo == null) {
            activoActivo = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getSerial() {
        return hardwareInfo != null ? hardwareInfo.getHwSerialNum() : "Sin serial";
    }
}
