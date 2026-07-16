package com.sistema.iTsystem.model;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "proveedores")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Proveedores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prov_id")
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long provId;

    @Column(name = "prov_nom", nullable = false, unique = true, length = 100)
    @ToString.Include
    private String provNom;

    @Column(name = "prov_descri", length = 255)
    private String provDescri;

    @Column(name = "prov_activo", nullable = false)
    private Boolean provActivo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (provActivo == null) {
            provActivo = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Proveedores proveedores)) {
            return false;
        }
        return provId != null && Objects.equals(provId, proveedores.provId);
    }

    @Override
    public int hashCode() {
        return provId != null ? Objects.hash(provId) : System.identityHashCode(this);
    }
}
