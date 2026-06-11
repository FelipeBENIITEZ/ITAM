package com.sistema.iTsystem.model;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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

    @OneToOne
    @JoinColumn(name = "activo_id", nullable = false, unique = true)
    private Activo activo;

    @ManyToOne
    @JoinColumn(name = "model_id", nullable = false)
    private Modelo modelo;

    @Column(name = "hw_serial_num", length = 100, unique = true, nullable = false)
    private String hwSerialNum;

    @Column(name = "hw_descri", columnDefinition = "TEXT")
    private String hwDescri;

    @OneToMany(mappedBy = "hardwareInfo", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Garantia> garantias;

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

    @Transient
    public Garantia getGarantia() {
        if (garantias == null || garantias.isEmpty()) {
            return null;
        }
        return garantias.stream()
            .max(Comparator.comparing(Garantia::getGaranFechaFin))
            .orElse(garantias.get(0));
    }

    public String getProveedorNombre() {
        return activo != null && activo.getProveedor() != null
            ? activo.getProveedor().getProvNom()
            : "Sin proveedor";
    }
}
