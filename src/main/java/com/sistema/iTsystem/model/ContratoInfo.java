
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
    
    // Métodos helper
    public boolean tieneArchivo() {
        return contratArchivoPath != null && !contratArchivoPath.isEmpty();
    }
    
    public String getProveedorNombre() {
        return proveedor != null ? proveedor.getProvNom() : "Sin proveedor";
    }
}