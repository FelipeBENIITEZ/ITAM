package com.sistema.iTsystem.dto.solicitudes;

import lombok.Data;

@Data
public class SolicitudFormDTO {
    private Long soliId;
    private Long tipoId;
    private Long activoId;
    private Long marcaId;
    private Long modelId;
    private Integer soliCantidad;
    private Long responsableId;
    private String soliMotivo;
    private String soliDescri;
}
