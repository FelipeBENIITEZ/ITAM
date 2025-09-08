package com.sistema.iTsystem.model;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAsignacionId implements Serializable {

    @Column(name = "usuario_us_id")
    private Long usuarioId;

    @Column(name = "activo_activo_id")
    private Long activoId;
}