package com.sistema.iTsystem.dto.activos;

import lombok.Data;

@Data
public class ActivoReferenciaDTO {
    private Long activoId;
    private String codigo;
    private String nombre;
    private String categoria;
    private String estado;
    private Long marcaId;
    private String marca;
    private Long modeloId;
    private String modelo;
    private String serial;
    private boolean asignado;
    private Long usuarioActualId;
    private String usuarioActual;
}
