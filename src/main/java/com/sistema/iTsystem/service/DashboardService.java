package com.sistema.iTsystem.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;

@Service
public class DashboardService {

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private HardwareInfoRepository hardwareRepository;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    public Map<String, Object> obtenerMetricasInventario() {
        Map<String, Object> metricas = new HashMap<>();
        metricas.put("totalActivos", activoRepository.count());
        metricas.put("totalHardware", hardwareRepository.count());
        metricas.put("asignacionesActivas", usuarioAsignacionRepository.findAll().stream()
            .filter(asignacion -> Boolean.TRUE.equals(asignacion.getAsignacionActiva()))
            .count());
        return metricas;
    }

    public Map<String, Object> obtenerMetricasGestion() {
        return new HashMap<>();
    }

    public Map<String, Object> obtenerMetricasAlertas() {
        return new HashMap<>();
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
