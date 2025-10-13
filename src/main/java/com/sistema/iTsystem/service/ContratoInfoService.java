package com.sistema.iTsystem.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.ContratoInfo;
import com.sistema.iTsystem.model.Proveedores;
import com.sistema.iTsystem.repository.ContratoInfoRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;

@Service
public class ContratoInfoService {

    @Autowired
    private ContratoInfoRepository contratoRepository;
    
    @Autowired
    private ProveedoresRepository proveedoresRepository;

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los contratos
     */
    public List<ContratoInfo> obtenerTodos() {
        return contratoRepository.findAll();
    }

    /**
     * Obtener todos los contratos ordenados por fecha de creación
     */
    public List<ContratoInfo> obtenerTodosOrdenados() {
        return contratoRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Obtener todos los contratos con información del proveedor (JOIN FETCH)
     */
    public List<ContratoInfo> obtenerTodosConProveedor() {
        return contratoRepository.findAllConProveedor();
    }

    /**
     * Buscar contrato por ID
     */
    public Optional<ContratoInfo> buscarPorId(Long id) {
        return contratoRepository.findById(id);
    }

    /**
     * Buscar contrato por número
     */
    public Optional<ContratoInfo> buscarPorNumero(String numero) {
        return contratoRepository.findByContratNumero(numero);
    }

    /**
     * Crear nuevo contrato
     */
    @Transactional
    public ContratoInfo crear(ContratoInfo contrato) {
        validarContrato(contrato);
        
        // Validar que el número de contrato no exista
        if (contratoRepository.existsByContratNumero(contrato.getContratNumero())) {
            throw new NumeroContratoDuplicadoException(
                "Ya existe un contrato con el número: " + contrato.getContratNumero()
            );
        }
        
        return contratoRepository.save(contrato);
    }

    /**
     * Actualizar contrato existente
     */
    @Transactional
    public ContratoInfo actualizar(Long id, ContratoInfo contratoActualizado) {
        ContratoInfo contratoExistente = contratoRepository.findById(id)
            .orElseThrow(() -> new ContratoNoEncontradoException(
                "Contrato con ID " + id + " no encontrado"
            ));
        
        validarContrato(contratoActualizado);
        
        // Validar número único (excepto el actual)
        if (!contratoExistente.getContratNumero().equals(contratoActualizado.getContratNumero())) {
            if (contratoRepository.existsByContratNumero(contratoActualizado.getContratNumero())) {
                throw new NumeroContratoDuplicadoException(
                    "Ya existe un contrato con el número: " + contratoActualizado.getContratNumero()
                );
            }
        }
        
        // Actualizar campos
        contratoExistente.setContratNumero(contratoActualizado.getContratNumero());
        contratoExistente.setContratDescripcion(contratoActualizado.getContratDescripcion());
        contratoExistente.setContratFechaInicio(contratoActualizado.getContratFechaInicio());
        contratoExistente.setContratFechaFin(contratoActualizado.getContratFechaFin());
        contratoExistente.setProveedor(contratoActualizado.getProveedor());
        
        return contratoRepository.save(contratoExistente);
    }

    /**
     * Eliminar contrato
     */
    @Transactional
    public void eliminar(Long id) {
        ContratoInfo contrato = contratoRepository.findById(id)
            .orElseThrow(() -> new ContratoNoEncontradoException(
                "Contrato con ID " + id + " no encontrado"
            ));
        
        // Validar que no tenga activos asociados
        if (tieneActivosAsociados(contrato)) {
            throw new ContratoConDependenciasException(
                "No se puede eliminar el contrato porque tiene activos asociados"
            );
        }
        
        contratoRepository.delete(contrato);
    }

    // ==================== BÚSQUEDAS AVANZADAS ====================

    /**
     * Buscar contratos por proveedor
     */
    public List<ContratoInfo> buscarPorProveedor(Long proveedorId) {
        return contratoRepository.findByProveedorId(proveedorId);
    }

    /**
     * Buscar contratos por descripción
     */
    public List<ContratoInfo> buscarPorDescripcion(String texto) {
        return contratoRepository.buscarPorDescripcion(texto);
    }

    /**
     * Buscar contratos sin archivo adjunto
     */
    public List<ContratoInfo> buscarSinArchivo() {
        return contratoRepository.findContratosSinArchivo();
    }

    /**
     * Buscar contratos con archivo adjunto
     */
    public List<ContratoInfo> buscarConArchivo() {
        List<ContratoInfo> todos = contratoRepository.findAll();
        return todos.stream()
            .filter(c -> c.getContratArchivoPath() != null && !c.getContratArchivoPath().isEmpty())
            .toList();
    }

    // ==================== GESTIÓN DE VIGENCIA ====================

    /**
     * Buscar contratos vigentes
     */
    public List<ContratoInfo> buscarVigentes() {
        LocalDate hoy = LocalDate.now();
        return contratoRepository.findAll().stream()
            .filter(c -> c.getContratFechaFin() == null || !c.getContratFechaFin().isBefore(hoy))
            .toList();
    }

    /**
     * Buscar contratos vencidos
     */
    public List<ContratoInfo> buscarVencidos() {
        LocalDate hoy = LocalDate.now();
        return contratoRepository.findAll().stream()
            .filter(c -> c.getContratFechaFin() != null && c.getContratFechaFin().isBefore(hoy))
            .toList();
    }

    /**
     * Buscar contratos próximos a vencer (30 días)
     */
    public List<ContratoInfo> buscarProximosAVencer() {
        LocalDate hoy = LocalDate.now();
        LocalDate dentroDe30Dias = hoy.plusDays(30);
        
        return contratoRepository.findAll().stream()
            .filter(c -> c.getContratFechaFin() != null)
            .filter(c -> !c.getContratFechaFin().isBefore(hoy))
            .filter(c -> !c.getContratFechaFin().isAfter(dentroDe30Dias))
            .toList();
    }

    /**
     * Verificar si un contrato está vigente
     */
    public boolean estaVigente(Long contratoId) {
        ContratoInfo contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new ContratoNoEncontradoException(
                "Contrato con ID " + contratoId + " no encontrado"
            ));
        
        LocalDate hoy = LocalDate.now();
        
        // Si no tiene fecha fin, se considera vigente
        if (contrato.getContratFechaFin() == null) {
            return true;
        }
        
        return !contrato.getContratFechaFin().isBefore(hoy);
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Contar contratos con archivo
     */
    public Long contarConArchivo() {
        return contratoRepository.countContratosConArchivo();
    }

    /**
     * Contar contratos sin archivo
     */
    public Long contarSinArchivo() {
        Long total = contratoRepository.count();
        Long conArchivo = contarConArchivo();
        return total - conArchivo;
    }

    /**
     * Contar contratos por proveedor
     */
    public Long contarPorProveedor(Long proveedorId) {
        return contratoRepository.countByProveedorId(proveedorId);
    }

    /**
     * Contar contratos vigentes
     */
    public long contarVigentes() {
        return buscarVigentes().size();
    }

    /**
     * Contar contratos vencidos
     */
    public long contarVencidos() {
        return buscarVencidos().size();
    }

    /**
     * Contar contratos próximos a vencer
     */
    public long contarProximosAVencer() {
        return buscarProximosAVencer().size();
    }

    // ==================== GESTIÓN DE ARCHIVOS ====================

    /**
     * Asignar archivo a contrato
     */
    @Transactional
    public ContratoInfo asignarArchivo(Long contratoId, String rutaArchivo) {
        ContratoInfo contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new ContratoNoEncontradoException(
                "Contrato con ID " + contratoId + " no encontrado"
            ));
        
        contrato.setContratArchivoPath(rutaArchivo);
        return contratoRepository.save(contrato);
    }

    /**
     * Eliminar archivo de contrato
     */
    @Transactional
    public ContratoInfo eliminarArchivo(Long contratoId) {
        ContratoInfo contrato = contratoRepository.findById(contratoId)
            .orElseThrow(() -> new ContratoNoEncontradoException(
                "Contrato con ID " + contratoId + " no encontrado"
            ));
        
        contrato.setContratArchivoPath(null);
        return contratoRepository.save(contrato);
    }

    // ==================== VALIDACIONES ====================

    /**
     * Verificar si el contrato tiene activos asociados
     */
    public boolean tieneActivosAsociados(ContratoInfo contrato) {
        // Esta validación se implementará cuando trabajemos con ContratoActivo
        // Por ahora retornar false
        return false;
    }

    /**
     * Verificar si existe un número de contrato
     */
    public boolean existeNumeroContrato(String numero) {
        return contratoRepository.existsByContratNumero(numero);
    }

    /**
     * Validar datos de contrato
     */
    private void validarContrato(ContratoInfo contrato) {
        if (contrato.getContratNumero() == null || contrato.getContratNumero().trim().isEmpty()) {
            throw new ContratoInvalidoException("El número de contrato es obligatorio");
        }
        
        if (contrato.getProveedor() == null) {
            throw new ContratoInvalidoException("El proveedor es obligatorio");
        }
        
        if (contrato.getContratFechaInicio() == null) {
            throw new ContratoInvalidoException("La fecha de inicio es obligatoria");
        }
        
        // Validar que fecha fin sea posterior a fecha inicio
        if (contrato.getContratFechaFin() != null && 
            contrato.getContratFechaFin().isBefore(contrato.getContratFechaInicio())) {
            throw new ContratoInvalidoException(
                "La fecha de fin no puede ser anterior a la fecha de inicio"
            );
        }
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    /**
     * Obtener todos los proveedores
     */
    public List<Proveedores> obtenerTodosProveedores() {
        return proveedoresRepository.findAll();
    }

    // ==================== EXCEPCIONES PERSONALIZADAS ====================

    public static class ContratoNoEncontradoException extends RuntimeException {
        public ContratoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ContratoInvalidoException extends RuntimeException {
        public ContratoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class NumeroContratoDuplicadoException extends RuntimeException {
        public NumeroContratoDuplicadoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ContratoConDependenciasException extends RuntimeException {
        public ContratoConDependenciasException(String mensaje) {
            super(mensaje);
        }
    }
}
