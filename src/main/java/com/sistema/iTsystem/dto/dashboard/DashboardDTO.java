package com.sistema.iTsystem.dto.dashboard;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class DashboardDTO {

    private Long totalActivos = 0L;
    private Long activosDisponibles = 0L;
    private Long activosAsignados = 0L;
    private Long activosEnMantenimiento = 0L;
    private Long solicitudesPendientes = 0L;
    private Long solicitudesEnAnalisis = 0L;

    private List<MetricaDashboardDTO> tarjetas = new ArrayList<>();
    private List<MetricaDashboardDTO> activosPorEstado = new ArrayList<>();
    private List<MetricaDashboardDTO> activosPorCategoria = new ArrayList<>();
    private List<MetricaDashboardDTO> solicitudesPorEstado = new ArrayList<>();
    private List<MetricaDashboardDTO> solicitudesAtencion = new ArrayList<>();
    private List<SolicitudRecienteDTO> solicitudesRecientes = new ArrayList<>();
}
