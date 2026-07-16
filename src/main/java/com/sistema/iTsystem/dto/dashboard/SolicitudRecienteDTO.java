package com.sistema.iTsystem.dto.dashboard;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class SolicitudRecienteDTO {

    private Long id;
    private LocalDateTime fecha;
    private String tipo;
    private String solicitante;
    private String responsable;
    private String estado;
    private String url;
    private String badgeClase;
}
