package com.sistema.iTsystem.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.LicenciaInfo;
import com.sistema.iTsystem.model.LicenciasEstados;
import com.sistema.iTsystem.model.LicenciasTipo;
import com.sistema.iTsystem.model.SoftwareInfo;
import com.sistema.iTsystem.repository.LicenciaInfoRepository;
import com.sistema.iTsystem.repository.LicenciaEstadosRepository;
import com.sistema.iTsystem.repository.LicenciaTipoRepository;
import com.sistema.iTsystem.repository.SoftwareInfoRepository;

@Service
public class LicenciaInfoService {

    @Autowired
    private LicenciaInfoRepository licenciaRepository;
    
    @Autowired
    private LicenciaEstadosRepository estadosRepository;
    
    @Autowired
    private LicenciaTipoRepository tiposRepository;
    
    @Autowired
    private SoftwareInfoRepository softwareRepository;

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todas las licencias
     */
    public List<LicenciaInfo> obtenerTodas() {
        return licenciaRepository.findAll();
    }

    /**
     * Buscar licencia por ID
     */
    public Optional<LicenciaInfo> buscarPorId(Long id) {
        return licenciaRepository.findById(id);
    }

    /**
     * Buscar licencia por ID con detalles
     */
    public LicenciaInfo buscarPorIdConDetalles(Long id) {
        return licenciaRepository.findByIdWithDetails(id);
    }

    /**
     * Crear nueva licencia
     */
    @Transactional
    public LicenciaInfo crear(LicenciaInfo licencia) {
        validarLicencia(licencia);
        
        // Asignar estado por defecto si no tiene
        if (licencia.getLicenciaEstado() == null) {
            LicenciasEstados estadoActiva = estadosRepository.findByLicEstadoNom("Activa")
                .orElseThrow(() -> new EstadoNoEncontradoException(
                    "Estado 'Activa' no encontrado"
                ));
            licencia.setLicenciaEstado(estadoActiva);
        }
        
        // Inicializar usos en 0 si no está definido
        if (licencia.getLicenciaUsos() == null) {
            licencia.setLicenciaUsos(0);
        }
        
        // Inicializar cupos en 1 si no está definido
        if (licencia.getLicenciaCupos() == null) {
            licencia.setLicenciaCupos(1);
        }
        
        return licenciaRepository.save(licencia);
    }

    /**
     * Actualizar licencia existente
     */
    @Transactional
    public LicenciaInfo actualizar(Long id, LicenciaInfo licenciaActualizada) {
        LicenciaInfo licenciaExistente = licenciaRepository.findById(id)
            .orElseThrow(() -> new LicenciaNoEncontradaException(
                "Licencia con ID " + id + " no encontrada"
            ));
        
        validarLicencia(licenciaActualizada);
        
        // Actualizar campos
        licenciaExistente.setLicenciaDescri(licenciaActualizada.getLicenciaDescri());
        licenciaExistente.setLicenciaIni(licenciaActualizada.getLicenciaIni());
        licenciaExistente.setLicenciaFin(licenciaActualizada.getLicenciaFin());
        licenciaExistente.setLicenciaCupos(licenciaActualizada.getLicenciaCupos());
        licenciaExistente.setLicenciaCosto(licenciaActualizada.getLicenciaCosto());
        licenciaExistente.setLicenciaEstado(licenciaActualizada.getLicenciaEstado());
        licenciaExistente.setLicenciaTipo(licenciaActualizada.getLicenciaTipo());
        
        return licenciaRepository.save(licenciaExistente);
    }

    /**
     * Eliminar licencia
     */
    @Transactional
    public void eliminar(Long id) {
        LicenciaInfo licencia = licenciaRepository.findById(id)
            .orElseThrow(() -> new LicenciaNoEncontradaException(
                "Licencia con ID " + id + " no encontrada"
            ));
        
        // Validar que no esté en uso
        if (licencia.getLicenciaUsos() > 0) {
            throw new LicenciaEnUsoException(
                "No se puede eliminar la licencia porque está en uso"
            );
        }
        
        licenciaRepository.delete(licencia);
    }

    // ==================== BÚSQUEDAS AVANZADAS ====================

    /**
     * Buscar licencias por software
     */
    public List<LicenciaInfo> buscarPorSoftware(Long softwareId) {
        return licenciaRepository.findBySoftwareInfo_SftId(softwareId);
    }

    /**
     * Buscar licencias por estado
     */
    public List<LicenciaInfo> buscarPorEstado(Long estadoId) {
        return licenciaRepository.findByLicenciaEstado_LicEstadoId(estadoId);
    }

    /**
     * Buscar licencias por tipo
     */
    public List<LicenciaInfo> buscarPorTipo(Long tipoId) {
        return licenciaRepository.findByLicenciaTipo_LicTipoId(tipoId);
    }

    /**
     * Buscar licencias vencidas
     */
    public List<LicenciaInfo> buscarVencidas() {
        return licenciaRepository.findLicenciasVencidas(LocalDate.now());
    }

    /**
     * Buscar licencias próximas a vencer (30 días)
     */
    public List<LicenciaInfo> buscarProximasAVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate dentroDe30Dias = hoy.plusDays(30);
        return licenciaRepository.findLicenciasProximasAVencer(hoy, dentroDe30Dias);
    }

    /**
     * Buscar licencias activas
     */
    public List<LicenciaInfo> buscarActivas() {
        return licenciaRepository.findLicenciasActivas(LocalDate.now());
    }

    /**
     * Buscar licencias con cupos disponibles
     */
    public List<LicenciaInfo> buscarConCuposDisponibles() {
        return licenciaRepository.findLicenciasConCuposDisponibles();
    }

    /**
     * Buscar licencias sin cupos
     */
    public List<LicenciaInfo> buscarSinCupos() {
        return licenciaRepository.findLicenciasSinCupos();
    }

    // ==================== GESTIÓN DE CUPOS ====================

    /**
     * Incrementar uso de licencia (cuando se instala)
     */
    @Transactional
    public boolean incrementarUso(Long licenciaId) {
        LicenciaInfo licencia = licenciaRepository.findById(licenciaId)
            .orElseThrow(() -> new LicenciaNoEncontradaException(
                "Licencia con ID " + licenciaId + " no encontrada"
            ));
        
        if (!licencia.tieneCuposDisponibles()) {
            throw new SinCuposDisponiblesException(
                "La licencia no tiene cupos disponibles"
            );
        }
        
        if (licencia.estaVencida()) {
            throw new LicenciaVencidaException(
                "No se puede usar una licencia vencida"
            );
        }
        
        licencia.setLicenciaUsos(licencia.getLicenciaUsos() + 1);
        licenciaRepository.save(licencia);
        
        return true;
    }

    /**
     * Decrementar uso de licencia (cuando se desinstala)
     */
    @Transactional
    public boolean decrementarUso(Long licenciaId) {
        LicenciaInfo licencia = licenciaRepository.findById(licenciaId)
            .orElseThrow(() -> new LicenciaNoEncontradaException(
                "Licencia con ID " + licenciaId + " no encontrada"
            ));
        
        if (licencia.getLicenciaUsos() <= 0) {
            throw new UsoInvalidoException(
                "La licencia no tiene usos registrados para decrementar"
            );
        }
        
        licencia.setLicenciaUsos(licencia.getLicenciaUsos() - 1);
        licenciaRepository.save(licencia);
        
        return true;
    }

    /**
     * Obtener cupos disponibles de una licencia
     */
    public int obtenerCuposDisponibles(Long licenciaId) {
        LicenciaInfo licencia = licenciaRepository.findById(licenciaId)
            .orElseThrow(() -> new LicenciaNoEncontradaException(
                "Licencia con ID " + licenciaId + " no encontrada"
            ));
        
        return licencia.getCuposDisponibles();
    }

    // ==================== ESTADÍSTICAS Y CÁLCULOS ====================

    /**
     * Contar licencias por estado
     */
    public List<Object[]> contarPorEstado() {
        return licenciaRepository.countLicenciasPorEstado();
    }

    /**
     * Calcular costo total de licencias
     */
    public BigDecimal calcularCostoTotal() {
        BigDecimal total = licenciaRepository.sumTotalCostoLicencias();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Contar total de cupos disponibles
     */
    public Long contarCuposDisponibles() {
        Long total = licenciaRepository.countTotalCuposDisponibles();
        return total != null ? total : 0L;
    }

    /**
     * Contar licencias vencidas
     */
    public long contarVencidas() {
        return buscarVencidas().size();
    }

    /**
     * Contar licencias próximas a vencer
     */
    public long contarProximasAVencer() {
        return buscarProximasAVencer().size();
    }

    // ==================== VALIDACIONES ====================

    /**
     * Verificar si una licencia puede ser usada
     */
    public boolean puedeSerUsada(Long licenciaId) {
        LicenciaInfo licencia = licenciaRepository.findById(licenciaId)
            .orElseThrow(() -> new LicenciaNoEncontradaException(
                "Licencia con ID " + licenciaId + " no encontrada"
            ));
        
        return !licencia.estaVencida() && licencia.tieneCuposDisponibles();
    }

    /**
     * Validar datos de licencia
     */
    private void validarLicencia(LicenciaInfo licencia) {
        if (licencia.getSoftwareInfo() == null) {
            throw new LicenciaInvalidaException("El software asociado es obligatorio");
        }
        
        if (licencia.getLicenciaIni() == null) {
            throw new LicenciaInvalidaException("La fecha de inicio es obligatoria");
        }
        
        if (licencia.getLicenciaTipo() == null) {
            throw new LicenciaInvalidaException("El tipo de licencia es obligatorio");
        }
        
        // Validar que fecha fin sea posterior a fecha inicio
        if (licencia.getLicenciaFin() != null && 
            licencia.getLicenciaFin().isBefore(licencia.getLicenciaIni())) {
            throw new LicenciaInvalidaException(
                "La fecha de fin no puede ser anterior a la fecha de inicio"
            );
        }
        
        // Validar que cupos sea positivo
        if (licencia.getLicenciaCupos() != null && licencia.getLicenciaCupos() < 1) {
            throw new LicenciaInvalidaException(
                "El número de cupos debe ser al menos 1"
            );
        }
        
        // Validar que costo sea positivo
        if (licencia.getLicenciaCosto() != null && 
            licencia.getLicenciaCosto().compareTo(BigDecimal.ZERO) < 0) {
            throw new LicenciaInvalidaException(
                "El costo no puede ser negativo"
            );
        }
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    /**
     * Obtener todos los estados de licencias
     */
    public List<LicenciasEstados> obtenerTodosEstados() {
        return estadosRepository.findAll();
    }

    /**
     * Obtener todos los tipos de licencias
     */
    public List<LicenciasTipo> obtenerTodosTipos() {
        return tiposRepository.findAll();
    }

    /**
     * Obtener todos los software
     */
    public List<SoftwareInfo> obtenerTodosSoftware() {
        return softwareRepository.findAll();
    }

    // ==================== EXCEPCIONES PERSONALIZADAS ====================

    public static class LicenciaNoEncontradaException extends RuntimeException {
        public LicenciaNoEncontradaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class LicenciaInvalidaException extends RuntimeException {
        public LicenciaInvalidaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class LicenciaEnUsoException extends RuntimeException {
        public LicenciaEnUsoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SinCuposDisponiblesException extends RuntimeException {
        public SinCuposDisponiblesException(String mensaje) {
            super(mensaje);
        }
    }

    public static class LicenciaVencidaException extends RuntimeException {
        public LicenciaVencidaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class UsoInvalidoException extends RuntimeException {
        public UsoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class EstadoNoEncontradoException extends RuntimeException {
        public EstadoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }
}