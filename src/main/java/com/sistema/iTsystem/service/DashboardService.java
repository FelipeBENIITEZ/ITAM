package com.sistema.iTsystem.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.ContratoInfoRepository;
import com.sistema.iTsystem.repository.EventosRepository;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.LicenciaInfoRepository;
import com.sistema.iTsystem.repository.MantenimientoRepository;
import com.sistema.iTsystem.repository.SoftwareInfoRepository;
import com.sistema.iTsystem.repository.SolicitudesRepository;

@Service
public class DashboardService {

    @Autowired
    private ActivoRepository activoRepository;
    
    @Autowired
    private HardwareInfoRepository hardwareRepository;
    
    @Autowired
    private SoftwareInfoRepository softwareRepository;
    
    @Autowired
    private LicenciaInfoRepository licenciaRepository;
    
    @Autowired
    private ContratoInfoRepository contratoRepository;
    
    @Autowired
    private EventosRepository eventosRepository;
    
    @Autowired
    private MantenimientoRepository mantenimientoRepository;
    
    @Autowired
    private SolicitudesRepository solicitudesRepository;

    // ==================== GRUPO 1: INVENTARIO ====================

    /**
     * Obtener métricas del Grupo 1: Inventario
     * - Total Activos por Categoría
     * - Hardware Disponible
     * - Software Instalado
     * - Licencias Activas
     */
    public Map<String, Object> obtenerMetricasInventario() {
        Map<String, Object> metricas = new HashMap<>();
        
        // Total de activos
        metricas.put("totalActivos", activoRepository.count());
        
        // Total de hardware
        metricas.put("totalHardware", hardwareRepository.count());
        
        // Total de software
        metricas.put("totalSoftware", softwareRepository.count());
        
        // Total de licencias activas
        LocalDate hoy = LocalDate.now();
        List<com.sistema.iTsystem.model.LicenciaInfo> licenciasActivas = 
            licenciaRepository.findLicenciasActivas(hoy);
        metricas.put("licenciasActivas", (long) licenciasActivas.size());
        
        return metricas;
    }

    // ==================== GRUPO 2: GESTIÓN ====================

    /**
     * Obtener métricas del Grupo 2: Gestión
     * - Solicitudes Pendientes
     * - Eventos Críticos
     * - Mantenimientos En Curso
     */
    public Map<String, Object> obtenerMetricasGestion() {
        Map<String, Object> metricas = new HashMap<>();
        
        // Solicitudes pendientes
        Long solicitudesPendientes = solicitudesRepository.countSolicitudesPendientes();
        metricas.put("solicitudesPendientes", solicitudesPendientes != null ? solicitudesPendientes : 0L);
        
        // Eventos críticos
        Long eventosCriticos = eventosRepository.countEventosCriticos();
        metricas.put("eventosCriticos", eventosCriticos != null ? eventosCriticos : 0L);
        
        // Mantenimientos en curso
        LocalDate hoy = LocalDate.now();
        Long mantenimientosEnCurso = mantenimientoRepository.countMantenimientosEnCurso(hoy);
        metricas.put("mantenimientosEnCurso", mantenimientosEnCurso != null ? mantenimientosEnCurso : 0L);
        
        return metricas;
    }

    // ==================== GRUPO 3: ALERTAS ====================

    /**
     * Obtener métricas del Grupo 3: Alertas
     * - Licencias a Vencer (30 días)
     * - Garantías Próximas a Vencer (30 días)
     * - Contratos Próximos a Vencer (30 días)
     */
    public Map<String, Object> obtenerMetricasAlertas() {
        Map<String, Object> metricas = new HashMap<>();
        
        LocalDate hoy = LocalDate.now();
        LocalDate dentroDe30Dias = hoy.plusDays(30);
        
        // Licencias próximas a vencer
        List<com.sistema.iTsystem.model.LicenciaInfo> licenciasProximas = 
            licenciaRepository.findLicenciasProximasAVencer(hoy, dentroDe30Dias);
        metricas.put("licenciasAVencer", (long) licenciasProximas.size());
        
        // Garantías próximas a vencer (se calculará cuando implementemos GarantiaService)
        // Por ahora en 0
        metricas.put("garantiasAVencer", 0L);
        
        // Contratos próximos a vencer
        List<com.sistema.iTsystem.model.ContratoInfo> contratos = contratoRepository.findAll();
        long contratosProximos = contratos.stream()
            .filter(c -> c.getContratFechaFin() != null)
            .filter(c -> !c.getContratFechaFin().isBefore(hoy))
            .filter(c -> !c.getContratFechaFin().isAfter(dentroDe30Dias))
            .count();
        metricas.put("contratosAVencer", contratosProximos);
        
        return metricas;
    }

    // ==================== GRÁFICO 1: ACTIVOS POR DEPARTAMENTO ====================

    /**
     * Obtener datos para gráfico de torta: Activos por Departamento
     */
    public List<Object[]> obtenerActivosPorDepartamento() {
        return activoRepository.countActivosPorDepartamento();
    }

    // ==================== GRÁFICO 2: DISTRIBUCIÓN POR TIPO ====================

    /**
     * Obtener datos para gráfico de torta: Distribución por Tipo de Activo
     * Retorna: Hardware, Software, Licencias
     */
    public Map<String, Long> obtenerDistribucionPorTipo() {
        Map<String, Long> distribucion = new HashMap<>();
        
        distribucion.put("Hardware", hardwareRepository.count());
        distribucion.put("Software", softwareRepository.count());
        distribucion.put("Licencias", licenciaRepository.count());
        
        return distribucion;
    }

    // ==================== GRÁFICO 3: ESTADOS DE ACTIVOS ====================

    /**
     * Obtener datos para gráfico de torta: Estados de Activos
     */
    public List<Object[]> obtenerActivosPorEstado() {
        return activoRepository.countActivosPorEstado();
    }

    // ==================== ESTADÍSTICAS ADICIONALES ====================

    /**
     * Obtener resumen financiero
     */
    public Map<String, BigDecimal> obtenerResumenFinanciero() {
        Map<String, BigDecimal> resumen = new HashMap<>();
        
        // Valor total de hardware
        BigDecimal valorHardware = hardwareRepository.sumTotalValorCompra();
        resumen.put("valorTotalHardware", valorHardware != null ? valorHardware : BigDecimal.ZERO);
        
        // Costo total de licencias
        BigDecimal costoLicencias = licenciaRepository.sumTotalCostoLicencias();
        resumen.put("costoTotalLicencias", costoLicencias != null ? costoLicencias : BigDecimal.ZERO);
        
        // Costo total de mantenimientos
        BigDecimal costoMantenimientos = mantenimientoRepository.sumTotalCostoMantenimientos();
        resumen.put("costoTotalMantenimientos", costoMantenimientos != null ? costoMantenimientos : BigDecimal.ZERO);
        
        // Total general
        BigDecimal total = valorHardware != null ? valorHardware : BigDecimal.ZERO;
        total = total.add(costoLicencias != null ? costoLicencias : BigDecimal.ZERO);
        total = total.add(costoMantenimientos != null ? costoMantenimientos : BigDecimal.ZERO);
        resumen.put("totalGeneral", total);
        
        return resumen;
    }

    /**
     * Obtener actividad reciente (últimos eventos, solicitudes, mantenimientos)
     */
    public Map<String, Object> obtenerActividadReciente() {
        Map<String, Object> actividad = new HashMap<>();
        
        // Últimos 5 eventos
        actividad.put("ultimosEventos", eventosRepository.findTop10ByOrderByEventFechaDesc());
        
        // Últimas 5 solicitudes
        actividad.put("ultimasSolicitudes", solicitudesRepository.findTop10ByOrderByCreatedAtDesc());
        
        // Últimos 5 mantenimientos
        actividad.put("ultimosMantenimientos", mantenimientoRepository.findTop10ByOrderByMantFechaIniDesc());
        
        return actividad;
    }

    /**
     * Obtener estadísticas por categoría de activo
     */
    public List<Object[]> obtenerActivosPorCategoria() {
        return activoRepository.countActivosPorCategoria();
    }

    /**
     * Obtener estadísticas de solicitudes por estado
     */
    public List<Object[]> obtenerSolicitudesPorEstado() {
        return solicitudesRepository.countSolicitudesPorEstado();
    }

    /**
     * Obtener estadísticas de eventos por nivel
     */
    public List<Object[]> obtenerEventosPorNivel() {
        return eventosRepository.countEventosPorNivel();
    }

    /**
     * Obtener estadísticas de mantenimientos por tipo
     */
    public List<Object[]> obtenerMantenimientosPorTipo() {
        return mantenimientoRepository.countMantenimientosPorTipo();
    }

    // ==================== DASHBOARD COMPLETO ====================

    /**
     * Obtener TODOS los datos del dashboard en un solo método
     */
    public Map<String, Object> obtenerDashboardCompleto() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Grupo 1: Inventario
        dashboard.put("inventario", obtenerMetricasInventario());
        
        // Grupo 2: Gestión
        dashboard.put("gestion", obtenerMetricasGestion());
        
        // Grupo 3: Alertas
        dashboard.put("alertas", obtenerMetricasAlertas());
        
        // Gráficos
        dashboard.put("activosPorDepartamento", obtenerActivosPorDepartamento());
        dashboard.put("distribucionPorTipo", obtenerDistribucionPorTipo());
        dashboard.put("activosPorEstado", obtenerActivosPorEstado());
        
        // Financiero
        dashboard.put("resumenFinanciero", obtenerResumenFinanciero());
        
        // Actividad reciente
        dashboard.put("actividadReciente", obtenerActividadReciente());
        
        return dashboard;
    }
}