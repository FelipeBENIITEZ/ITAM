package com.sistema.iTsystem.model;

import java.time.LocalDateTime;

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
@Table(name = "persona")
@Data
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "per_id")
    @EqualsAndHashCode.Include
    private Long perId;

    @Column(name = "per_nom_1", nullable = false, length = 100)
    private String perNom1;

    @Column(name = "per_nom_2", length = 100)
    private String perNom2;

    @Column(name = "per_ape_1", nullable = false, length = 100)
    private String perApe1;

    @Column(name = "per_ape_2", length = 100)
    private String perApe2;

    @Column(name = "per_ci", unique = true, nullable = false, length = 20)
    private String perCi;

    @Column(name = "per_activo", nullable = false)
    private Boolean perActivo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (perActivo == null) {
            perActivo = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
