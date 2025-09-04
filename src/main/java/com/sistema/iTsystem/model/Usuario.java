package com.sistema.iTsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario")
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usu_id")
    private Long usuId;

    @Column(name = "usu_login", unique = true, nullable = false, length = 50)
    private String usuLogin;

    @Column(name = "usu_password", nullable = false)
    private String usuPassword;

    @Column(name = "usu_mail", unique = true, nullable = false, length = 100)
    private String usuMail;

    // Relación con Persona
    @ManyToOne
    @JoinColumn(name = "per_id")
    private Persona persona;

    // Relación con Rol
    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;

    // Relación con Departamentos
    @ManyToOne
    @JoinColumn(name = "dept_id")
    private Departamentos departamento;
}
