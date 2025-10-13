package com.sistema.iTsystem.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Mantenimiento;
import com.sistema.iTsystem.model.MantenimientoTipo;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.MantenimientoRepository;
import com.sistema.iTsystem.repository.MantenimientoTipoRepository;

@Service
public class MantenimientoService {

    // ==================== EXCEPCIONES PERSONALIZADAS (AL INICIO) ====================

    public static class MantenimientoNoEncontradoException extends RuntimeException {
        public MantenimientoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class MantenimientoInvalidoException extends RuntimeException {
        public MantenimientoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class MantenimientoYaFinalizadoException extends RuntimeException {
        public MantenimientoYaFinalizadoException(String mensaje) {
            super(mensaje);
        }
    }

    // ==================== AUTOWIRED ====================

    @Autowired
    private MantenimientoRepository mantenimientoRepository;
    
    @Autowired
    private MantenimientoTipoRepository tipoRepository;
    
    @Autowired
    private HardwareInfoRepository hardwareRepository;

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los mantenimientos
     */
    public List<Mantenimiento> obtenerTodos() {
        return mantenimientoRepository.findAll();
    }

    /**
     * Obtener todos los mantenimientos con paginación
     */
    public Page<Mantenimiento> obtenerTodos(Pageable pageable) {
        return mantenimientoRepository.findAllByOrderByMantFechaIniDesc(pageable);
    }

    /**
     * Buscar mantenimiento por ID
     */
    public Optional<Mantenimiento> buscarPorId(Long id) {
        return mantenimientoRepository.findById(id);
    }

    /**
     * Buscar mantenimiento por ID con detalles (JOIN FETCH)
     */
    public Mantenimiento buscarPorIdConDetalles(Long id) {
        return mantenimientoRepository.findByIdWithDetails(id);
    }

    /**
     * Crear nuevo mantenimiento
     */
    @Transactional
    public Mantenimiento crear(Mantenimiento mantenimiento) {
        validarMantenimiento(mantenimiento);
        
        // Si no tiene fecha inicio, asignar hoy
        if (mantenimiento.getMantFechaIni() == null) {
            mantenimiento.setMantFechaIni(LocalDate.now());
        }
        
        return mantenimientoRepository.save(mantenimiento);
    }

    /**
     * Actualizar mantenimiento existente
     */
    @Transactional
    public Mantenimiento actualizar(Long id, Mantenimiento mantenimientoActualizado) {
        Mantenimiento mantenimientoExistente = mantenimientoRepository.findById(id)
            .orElseThrow(() -> new MantenimientoNoEncontradoException(
                "Mantenimiento con ID " + id + " no encontrado"
            ));
        
        validarMantenimiento(mantenimientoActualizado);
        
        // Actualizar campos
        mantenimientoExistente.setMantFechaIni(mantenimientoActualizado.getMantFechaIni());
        mantenimientoExistente.setMantFechaFin(mantenimientoActualizado.getMantFechaFin());
        mantenimientoExistente.setMantDescri(mantenimientoActualizado.getMantDescri());
        mantenimientoExistente.setMantCosto(mantenimientoActualizado.getMantCosto());
        mantenimientoExistente.setMantenimientoTipo(mantenimientoActualizado.getMantenimientoTipo());
        
        return mantenimientoRepository.save(mantenimientoExistente);
    }

    /**
     * Eliminar mantenimiento
     */
    @Transactional
    public void eliminar(Long id) {
        Mantenimiento mantenimiento = mantenimientoRepository.findById(id)
            .orElseThrow(() -> new MantenimientoNoEncontradoException(
                "Mantenimiento con ID " + id + " no encontrado"
            ));
        
        mantenimientoRepository.delete(mantenimiento);
    }

    /**
     * Finalizar mantenimiento (poner fecha fin)
     */
    @Transactional
    public Mantenimiento finalizar(Long id, LocalDate fechaFin) {
        Mantenimiento mantenimiento = mantenimientoRepository.findById(id)
            .orElseThrow(() -> new MantenimientoNoEncontradoException(
                "Mantenimiento con ID " + id + " no encontrado"
            ));
        
        if (mantenimiento.getMantFechaFin() != null) {
            throw new MantenimientoYaFinalizadoException(
                "El mantenimiento ya está finalizado"
            );
        }
        
        if (fechaFin == null) {
            fechaFin = LocalDate.now();
        }
        
        if (fechaFin.isBefore(mantenimiento.getMantFechaIni())) {
            throw new MantenimientoInvalidoException(
                "La fecha de finalización no puede ser anterior a la fecha de inicio"
            );
        }
        
        mantenimiento.setMantFechaFin(fechaFin);
        return mantenimientoRepository.save(mantenimiento);
    }

    // ==================== BÚSQUEDAS POR HARDWARE ====================

    public List<Mantenimiento> buscarPorHardware(Long hardwareId) {
        return mantenimientoRepository.findByHardwareInfo_HwIdOrderByMantFechaIniDesc(hardwareId);
    }

    public Page<Mantenimiento> buscarPorHardware(Long hardwareId, Pageable pageable) {
        return mantenimientoRepository.findByHardwareInfo_HwId(hardwareId, pageable);
    }

    public List<Mantenimiento> buscarPorHardwareConDetalles(Long hardwareId) {
        return mantenimientoRepository.findByHardwareIdWithDetails(hardwareId);
    }

    // ==================== BÚSQUEDAS POR TIPO ====================

    public List<Mantenimiento> buscarPorTipo(Long tipoId) {
        return mantenimientoRepository.findByMantenimientoTipo_MantTipoId(tipoId);
    }

    public Page<Mantenimiento> buscarPorTipo(Long tipoId, Pageable pageable) {
        return mantenimientoRepository.findByMantenimientoTipo_MantTipoId(tipoId, pageable);
    }

    public List<Mantenimiento> buscarPreventivos() {
        return mantenimientoRepository.findMantenimientosPreventivos();
    }

    public List<Mantenimiento> buscarCorrectivos() {
        return mantenimientoRepository.findMantenimientosCorrectivos();
    }

    // ==================== BÚSQUEDAS POR ESTADO ====================

    public List<Mantenimiento> buscarEnCurso() {
        return mantenimientoRepository.findMantenimientosEnCurso(LocalDate.now());
    }

    public List<Mantenimiento> buscarFinalizados() {
        return mantenimientoRepository.findMantenimientosFinalizados(LocalDate.now());
    }

    // ==================== BÚSQUEDAS POR FECHA ====================

    public List<Mantenimiento> buscarPorFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        return mantenimientoRepository.findByFechaInicioBetween(fechaInicio, fechaFin);
    }

    public List<Mantenimiento> buscarDelMesActual() {
        LocalDate hoy = LocalDate.now();
        int mes = hoy.getMonthValue();
        int anio = hoy.getYear();
        return mantenimientoRepository.findMantenimientosDelMes(mes, anio);
    }

    // ==================== BÚSQUEDAS POR COSTO ====================

    public List<Mantenimiento> buscarSinCosto() {
        return mantenimientoRepository.findMantenimientosSinCosto();
    }

    public List<Mantenimiento> buscarPorCostoMayorA(BigDecimal costoMinimo) {
        return mantenimientoRepository.findByMantCostoGreaterThan(costoMinimo);
    }

    public List<Mantenimiento> buscarMasCostosos() {
        return mantenimientoRepository.findTop10ByOrderByMantCostoDesc();
    }

    // ==================== BÚSQUEDAS POR DESCRIPCIÓN ====================

    public List<Mantenimiento> buscarPorDescripcion(String texto) {
        return mantenimientoRepository.findByMantDescriContaining(texto);
    }

    // ==================== BÚSQUEDAS COMBINADAS ====================

    public List<Mantenimiento> buscarPorHardwareYTipo(Long hardwareId, Long tipoId) {
        return mantenimientoRepository.findByHardwareInfo_HwIdAndMantenimientoTipo_MantTipoId(
            hardwareId, tipoId
        );
    }

    // ==================== ESTADÍSTICAS Y CÁLCULOS ====================

    public BigDecimal calcularCostoTotal() {
        BigDecimal total = mantenimientoRepository.sumTotalCostoMantenimientos();
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal calcularCostoPorHardware(Long hardwareId) {
        BigDecimal total = mantenimientoRepository.sumCostosPorHardware(hardwareId);
        return total != null ? total : BigDecimal.ZERO;
    }

    public BigDecimal calcularCostoPromedio() {
        BigDecimal promedio = mantenimientoRepository.avgCostoMantenimiento();
        return promedio != null ? promedio : BigDecimal.ZERO;
    }

    public List<Object[]> contarPorTipo() {
        return mantenimientoRepository.countMantenimientosPorTipo();
    }

    public List<Object[]> contarPorHardware() {
        return mantenimientoRepository.countMantenimientosPorHardware();
    }

    public List<Object[]> contarPorMes() {
        int anioActual = LocalDate.now().getYear();
        return mantenimientoRepository.countMantenimientosPorMes(anioActual);
    }

    public Long contarEnCurso() {
        return mantenimientoRepository.countMantenimientosEnCurso(LocalDate.now());
    }

    public Long contarPorHardwareId(Long hardwareId) {
        return mantenimientoRepository.countMantenimientosPorHardwareId(hardwareId);
    }

    public Long contarSinCosto() {
        return mantenimientoRepository.countMantenimientosSinCosto();
    }

    public List<Mantenimiento> obtenerUltimos(int cantidad) {
        return mantenimientoRepository.findTop10ByOrderByMantFechaIniDesc();
    }

    // ==================== VALIDACIONES ====================

    public boolean tieneMantenimientosEnCurso(Long hardwareId) {
        List<Mantenimiento> enCurso = mantenimientoRepository.findMantenimientosEnCurso(LocalDate.now());
        return enCurso.stream()
            .anyMatch(m -> m.getHardwareInfo().getHwId().equals(hardwareId));
    }

    private void validarMantenimiento(Mantenimiento mantenimiento) {
        if (mantenimiento.getHardwareInfo() == null) {
            throw new MantenimientoInvalidoException("El hardware es obligatorio");
        }
        
        if (mantenimiento.getMantenimientoTipo() == null) {
            throw new MantenimientoInvalidoException("El tipo de mantenimiento es obligatorio");
        }
        
        if (mantenimiento.getMantFechaIni() == null) {
            throw new MantenimientoInvalidoException("La fecha de inicio es obligatoria");
        }
        
        if (mantenimiento.getMantFechaFin() != null && 
            mantenimiento.getMantFechaFin().isBefore(mantenimiento.getMantFechaIni())) {
            throw new MantenimientoInvalidoException(
                "La fecha de finalización no puede ser anterior a la fecha de inicio"
            );
        }
        
        if (mantenimiento.getMantCosto() != null && 
            mantenimiento.getMantCosto().compareTo(BigDecimal.ZERO) < 0) {
            throw new MantenimientoInvalidoException(
                "El costo no puede ser negativo"
            );
        }
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    public List<MantenimientoTipo> obtenerTodosTipos() {
        return tipoRepository.findAll();
    }

    public List<HardwareInfo> obtenerTodoHardware() {
        return hardwareRepository.findAll();
    }
}