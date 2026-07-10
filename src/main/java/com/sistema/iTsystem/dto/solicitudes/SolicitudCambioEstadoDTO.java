package com.sistema.iTsystem.dto.solicitudes;

import lombok.Data;

@Data
public class SolicitudCambioEstadoDTO {
    private Long nuevoEstadoId;
    private Long responsableId;
    private String observacion;
}
