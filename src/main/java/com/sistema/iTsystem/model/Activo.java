
package com.sistema.iTsystem.model;

import java.time.LocalDateTime;

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
@Table(name = "activo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Activo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activo_id")
    private Long activoId;

    @Column(name = "activo_nom", nullable = false, length = 150)
    private String activoNom;

    @Column(name = "activo_fecha_ingreso")
    private LocalDateTime activoFechaIngreso;

    @Column(name = "activo_fecha_egreso")
    private LocalDateTime activoFechaEgreso;

    // Relación opcional con ContratoInfo
    @ManyToOne
    @JoinColumn(name = "contrat_id")
    private ContratoInfo contrato;

    // Relación obligatoria con Departamentos
    @ManyToOne
    @JoinColumn(name = "dept_id", nullable = false)
    private Departamentos departamento;

    // Relación obligatoria con CategoriasActivo
    @ManyToOne
    @JoinColumn(name = "cat_id", nullable = false)
    private CategoriasActivo categoria;

    // Relación obligatoria con EstadoActivo
    @ManyToOne
    @JoinColumn(name = "estado_id", nullable = false)
    private EstadoActivo estado;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (activoFechaIngreso == null) {
            activoFechaIngreso = LocalDateTime.now();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}