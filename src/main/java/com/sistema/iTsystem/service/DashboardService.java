package com.sistema.iTsystem.service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @Autowired
    private ActivoService activoService;

    public Map<String, Object> obtenerResumenDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        Map<String, Long> estados = new LinkedHashMap<>();

        for (Object[] fila : activoService.contarPorEstado()) {
            estados.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }

        dashboard.put("totalActivos", activoService.contarTodos());
        dashboard.put("activosDisponibles", estados.getOrDefault("Disponible", 0L));
        dashboard.put("activosEnMantenimiento", estados.getOrDefault("En mantenimiento", 0L));
        dashboard.put("activosDadosDeBaja", estados.getOrDefault("Dado de baja", 0L));
        dashboard.put("activosPorEstado", estados);
        dashboard.put("activosPorCategoria", activoService.contarPorCategoria());
        return dashboard;
    }

    public List<Object[]> obtenerActivosPorCategoria() {
        return activoService.contarPorCategoria();
    }

    public Map<String, Long> obtenerActivosPorEstado() {
        Map<String, Long> estados = new LinkedHashMap<>();

        for (Object[] fila : activoService.contarPorEstado()) {
            estados.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }

        return estados;
    }
}
