package com.sistema.iTsystem.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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

    @OneToOne
    @JoinColumn(name = "per_id", nullable = false, unique = true)
    private Persona persona;

    @ManyToOne
    @JoinColumn(name = "dept_id", nullable = false)
    private Departamentos departamento;

    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false)
    private Rol rol;

    @Column(name = "usu_activo", nullable = false)
    private Boolean usuActivo = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (usuActivo == null) {
            usuActivo = true;
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean esAdministrador() {
        return rol != null && rol.getRolId() != null && rol.getRolId().equals(1L);
    }
}
