package com.sistema.iTsystem.model;

import java.time.LocalDate;
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
@Table(name = "contrato_info")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContratoInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contrat_id")
    private Long contratId;

    @Column(name = "contrat_numero", length = 100, unique = true, nullable = false)
    private String contratNumero;

    @Column(name = "contrat_descripcion", columnDefinition = "TEXT", nullable = false)
    private String contratDescripcion;

    //AGREGAR ESTAS DOS FECHAS:
    @Column(name = "contrat_fecha_inicio")
    private LocalDate contratFechaInicio;

    @Column(name = "contrat_fecha_fin")
    private LocalDate contratFechaFin;

    // Relación con Proveedor (OBLIGATORIO)
    @ManyToOne
    @JoinColumn(name = "prov_id", nullable = false)
    private Proveedores proveedor;

    @Column(name = "contrat_archivo_path", length = 500)
    private String contratArchivoPath;

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
    
    public boolean tieneArchivo() {
        return contratArchivoPath != null && !contratArchivoPath.isEmpty();
    }
    
    public String getProveedorNombre() {
        return proveedor != null ? proveedor.getProvNom() : "Sin proveedor";
    }
    
    /**
     * Verifica si el contrato está vigente
     */
    public boolean estaVigente() {
        LocalDate hoy = LocalDate.now();
        
        if (contratFechaFin == null) {
            return true; // Sin fecha fin, se considera vigente
        }
        
        return !contratFechaFin.isBefore(hoy);
    }
    
    /**
     * Verifica si el contrato está vencido
     */
    public boolean estaVencido() {
        return !estaVigente();
    }
    
    /**
     * Verifica si el contrato está próximo a vencer (30 días o menos)
     */
    public boolean proximoAVencer() {
        if (contratFechaFin == null) {
            return false;
        }
        
        LocalDate hoy = LocalDate.now();
        LocalDate dentroDe30Dias = hoy.plusDays(30);
        
        return !contratFechaFin.isBefore(hoy) && !contratFechaFin.isAfter(dentroDe30Dias);
    }
}