package com.sistema.iTsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "categorias_activo")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoriasActivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cat_id")
    private Long catId;

    @Column(name = "cat_nom", nullable = false, length = 100)
    private String catNom;

    @Column(name = "cat_descri", length = 255)
    private String catDescri;
}