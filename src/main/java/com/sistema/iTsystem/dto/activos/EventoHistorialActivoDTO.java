package com.sistema.iTsystem.dto.activos;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class EventoHistorialActivoDTO {
    private LocalDateTime fecha;
    private String tipoEvento;
    private String titulo;
    private String descripcion;
    private String estadoAnterior;
    private String estadoNuevo;
    private String usuarioAnterior;
    private String usuarioNuevo;
    private String usuarioEjecutor;
    private String motivo;
    private String observacion;
    private Long solicitudId;
    private Integer prioridadOrden;
}
