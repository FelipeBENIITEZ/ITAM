package com.sistema.iTsystem.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.PersonaRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.SolicitudesService;

@Service
public class DashboardService {

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private HardwareInfoRepository hardwareRepository;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private SolicitudesService solicitudesService;

    public Map<String, Object> obtenerMetricasInventario() {
        Map<String, Object> metricas = new HashMap<>();
        metricas.put("totalActivos", activoRepository.count());
        metricas.put("activosPorEstado", activoRepository.countActivosPorEstado().stream()
            .collect(Collectors.toMap(row -> String.valueOf(row[0]), row -> ((Number) row[1]).longValue())));
        metricas.put("activosPorCategoria", activoRepository.countActivosPorCategoria());
        metricas.put("totalHardware", hardwareRepository.count());
        metricas.put("asignacionesActivas", usuarioAsignacionRepository.findByAsignacionActivaTrueOrderByAsignacionFechaDesc().size());
        return metricas;
    }

    public Map<String, Object> obtenerMetricasGestion() {
        Map<String, Object> metricas = new HashMap<>();
        metricas.put("totalUsuarios", usuarioRepository.count());
        metricas.put("totalPersonas", personaRepository.count());
        metricas.put("totalDepartamentos", 0L);
        return metricas;
    }

    public Map<String, Object> obtenerMetricasAlertas() {
        Map<String, Object> metricas = new HashMap<>();
        Map<String, Long> estados = activoRepository.countActivosPorEstado().stream()
            .collect(Collectors.toMap(row -> String.valueOf(row[0]), row -> ((Number) row[1]).longValue()));
        metricas.put("incidenciasAbiertas", solicitudesService.contarPendientes());
        metricas.put("activosEnMantenimiento", estados.getOrDefault("En mantenimiento", 0L));
        metricas.put("activosDadosDeBaja", estados.getOrDefault("Dado de baja", 0L));
        return metricas;
    }

    public List<Object[]> obtenerActivosPorDepartamento() {
        return activoRepository.countActivosPorDepartamento();
    }

    public Map<String, Long> obtenerDistribucionPorTipo() {
        Map<String, Long> distribucion = new HashMap<>();
        distribucion.put("Hardware", hardwareRepository.count());
        return distribucion;
    }

    public List<Object[]> obtenerActivosPorEstado() {
        return activoRepository.countActivosPorEstado();
    }

    public Map<String, BigDecimal> obtenerResumenFinanciero() {
        Map<String, BigDecimal> resumen = new HashMap<>();
        resumen.put("valorTotalHardware", BigDecimal.ZERO);
        resumen.put("totalGeneral", BigDecimal.ZERO);
        return resumen;
    }

    public Map<String, Object> obtenerActividadReciente() {
        return new HashMap<>();
    }

    public List<Object[]> obtenerActivosPorCategoria() {
        return activoRepository.countActivosPorCategoria();
    }

    public List<Object[]> obtenerSolicitudesPorEstado() {
        return List.of();
    }

    public List<Object[]> obtenerEventosPorNivel() {
        return List.of();
    }

    public List<Object[]> obtenerMantenimientosPorTipo() {
        return List.of();
    }

    public Map<String, Object> obtenerDashboardCompleto() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("inventario", obtenerMetricasInventario());
        dashboard.put("gestion", obtenerMetricasGestion());
        dashboard.put("alertas", obtenerMetricasAlertas());
        dashboard.put("activosPorDepartamento", obtenerActivosPorDepartamento());
        dashboard.put("distribucionPorTipo", obtenerDistribucionPorTipo());
        dashboard.put("activosPorEstado", obtenerActivosPorEstado());
        dashboard.put("resumenFinanciero", obtenerResumenFinanciero());
        dashboard.put("actividadReciente", obtenerActividadReciente());
        return dashboard;
    }
}
