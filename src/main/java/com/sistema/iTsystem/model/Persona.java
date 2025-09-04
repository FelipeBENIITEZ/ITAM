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
@Table(name = "persona")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Persona {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "per_id")
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
}
