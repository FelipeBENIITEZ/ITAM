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

@Entity
@Table(name = "marcas")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "marca_id")
    @EqualsAndHashCode.Include
    private Long marcaId;

    @Column(name = "marca_nom", nullable = false, unique = true, length = 100)
    private String marcaNom;

    @Column(name = "marca_descri", length = 255)
    private String marcaDescri;

    @Column(name = "marca_activa", nullable = false)
    private Boolean marcaActiva = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (marcaActiva == null) {
            marcaActiva = true;
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
        if (!(o instanceof Marca marca)) {
            return false;
        }
        return marcaId != null && Objects.equals(marcaId, marca.marcaId);
    }

    @Override
    public int hashCode() {
        return marcaId != null ? Objects.hash(marcaId) : System.identityHashCode(this);
    }
}
