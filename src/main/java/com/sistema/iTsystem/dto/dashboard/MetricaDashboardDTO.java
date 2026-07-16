package com.sistema.iTsystem.dto.dashboard;

import lombok.Data;

@Data
public class MetricaDashboardDTO {

    private String codigo;
    private String nombre;
    private Long cantidad = 0L;
    private Double porcentaje = 0.0;
    private String url;
    private String iconoClase;
    private String colorClase;
}
