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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categoria_estado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cat_estado_id")
    private Long catEstadoId;

    @ManyToOne
    @JoinColumn(name = "categoria_cat_id", nullable = false)
    private CategoriasActivo categoria;

    @ManyToOne
    @JoinColumn(name = "estado_estado_id", nullable = false)
    private EstadoActivo estado;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}