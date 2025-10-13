package com.sistema.iTsystem.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Modelo;
import com.sistema.iTsystem.model.Proveedores;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.ModeloRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;

@Service
public class HardwareInfoService {

    @Autowired
    private HardwareInfoRepository hardwareRepository;
    
    @Autowired
    private ModeloRepository modeloRepository;
    
    @Autowired
    private ProveedoresRepository proveedoresRepository;

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los hardware
     */
    public List<HardwareInfo> obtenerTodos() {
        return hardwareRepository.findAll();
    }

    /**
     * Buscar hardware por ID
     */
    public Optional<HardwareInfo> buscarPorId(Long id) {
        return hardwareRepository.findById(id);
    }

    /**
     * Buscar hardware por ID con detalles (JOIN FETCH)
     */
    public Optional<HardwareInfo> buscarPorIdConDetalles(Long id) {
        return hardwareRepository.findByIdWithDetails(id);
    }

    /**
     * Buscar hardware por activo ID
     */
    public Optional<HardwareInfo> buscarPorActivoId(Long activoId) {
        return hardwareRepository.findByActivo_ActivoId(activoId);
    }

    /**
     * Buscar hardware por activo ID con detalles
     */
    public Optional<HardwareInfo> buscarPorActivoIdConDetalles(Long activoId) {
        return hardwareRepository.findByActivoIdWithDetails(activoId);
    }

    /**
     * Buscar hardware por serial
     */
    public Optional<HardwareInfo> buscarPorSerial(String serial) {
        return hardwareRepository.findByHwSerialNum(serial);
    }

    /**
     * Crear nuevo hardware
     */
    @Transactional
    public HardwareInfo crear(HardwareInfo hardware) {
        validarHardware(hardware);
        
        // Validar que el serial no exista
        if (hardwareRepository.existsByHwSerialNum(hardware.getHwSerialNum())) {
            throw new SerialDuplicadoException(
                "Ya existe un hardware con el serial: " + hardware.getHwSerialNum()
            );
        }
        
        return hardwareRepository.save(hardware);
    }

    /**
     * Actualizar hardware existente
     */
    @Transactional
    public HardwareInfo actualizar(Long id, HardwareInfo hardwareActualizado) {
        HardwareInfo hardwareExistente = hardwareRepository.findById(id)
            .orElseThrow(() -> new HardwareNoEncontradoException(
                "Hardware con ID " + id + " no encontrado"
            ));
        
        validarHardware(hardwareActualizado);
        
        // Validar serial único (excepto el actual)
        if (!hardwareExistente.getHwSerialNum().equals(hardwareActualizado.getHwSerialNum())) {
            if (hardwareRepository.existsByHwSerialNum(hardwareActualizado.getHwSerialNum())) {
                throw new SerialDuplicadoException(
                    "Ya existe un hardware con el serial: " + hardwareActualizado.getHwSerialNum()
                );
            }
        }
        
        // Actualizar campos
        hardwareExistente.setHwSerialNum(hardwareActualizado.getHwSerialNum());
        hardwareExistente.setHwDescri(hardwareActualizado.getHwDescri());
        hardwareExistente.setHwValorCompra(hardwareActualizado.getHwValorCompra());
        hardwareExistente.setModelo(hardwareActualizado.getModelo());
        hardwareExistente.setProveedor(hardwareActualizado.getProveedor());
        
        return hardwareRepository.save(hardwareExistente);
    }

    /**
     * Eliminar hardware
     */
    @Transactional
    public void eliminar(Long id) {
        HardwareInfo hardware = hardwareRepository.findById(id)
            .orElseThrow(() -> new HardwareNoEncontradoException(
                "Hardware con ID " + id + " no encontrado"
            ));
        
        hardwareRepository.delete(hardware);
    }

    // ==================== BÚSQUEDAS AVANZADAS ====================

    /**
     * Buscar hardware por modelo
     */
    public List<HardwareInfo> buscarPorModelo(Long modeloId) {
        return hardwareRepository.findByModelo_ModelId(modeloId);
    }

    /**
     * Buscar hardware por proveedor
     */
    public List<HardwareInfo> buscarPorProveedor(Long provId) {
        return hardwareRepository.findByProveedor_ProvId(provId);
    }

    /**
     * Buscar hardware sin proveedor asignado
     */
    public List<HardwareInfo> buscarSinProveedor() {
        return hardwareRepository.findHardwareSinProveedor();
    }

    /**
     * Buscar hardware sin valor de compra
     */
    public List<HardwareInfo> buscarSinValorCompra() {
        return hardwareRepository.findHardwareSinValorCompra();
    }

    /**
     * Buscar hardware con valor mayor a X
     */
    public List<HardwareInfo> buscarPorValorMayorA(BigDecimal valor) {
        return hardwareRepository.findByValorCompraGreaterThan(valor);
    }

    /**
     * Buscar hardware con valor entre X y Y
     */
    public List<HardwareInfo> buscarPorValorEntre(BigDecimal min, BigDecimal max) {
        return hardwareRepository.findByValorCompraBetween(min, max);
    }

    // ==================== CÁLCULOS Y ESTADÍSTICAS ====================

    /**
     * Calcular valor total de hardware
     */
    public BigDecimal calcularValorTotal() {
        BigDecimal total = hardwareRepository.sumTotalValorCompra();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Calcular valor promedio de hardware
     */
    public BigDecimal calcularValorPromedio() {
        BigDecimal promedio = hardwareRepository.avgValorCompra();
        return promedio != null ? promedio : BigDecimal.ZERO;
    }

    /**
     * Contar hardware por proveedor
     */
    public List<Object[]> contarPorProveedor() {
        return hardwareRepository.countHardwarePorProveedor();
    }

    /**
     * Contar hardware por modelo
     */
    public List<Object[]> contarPorModelo() {
        return hardwareRepository.countHardwarePorModelo();
    }

    /**
     * Contar hardware sin valor de compra
     */
    public long contarSinValorCompra() {
        return hardwareRepository.countHardwareSinValorCompra();
    }

    // ==================== VERIFICACIONES ====================

    /**
     * Verificar si existe un serial
     */
    public boolean existeSerial(String serial) {
        return hardwareRepository.existsByHwSerialNum(serial);
    }

    /**
     * Verificar si el hardware tiene garantía vigente
     */
    public boolean tieneGarantiaVigente(HardwareInfo hardware) {
        if (hardware.getGarantia() == null) {
            return false;
        }
        return hardware.getGarantia().isVigente();
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    /**
     * Obtener todos los modelos
     */
    public List<Modelo> obtenerTodosModelos() {
        return modeloRepository.findAll();
    }

    /**
     * Obtener todos los proveedores
     */
    public List<Proveedores> obtenerTodosProveedores() {
        return proveedoresRepository.findAll();
    }

    // ==================== VALIDACIONES ====================

    /**
     * Validar datos de hardware
     */
    private void validarHardware(HardwareInfo hardware) {
        if (hardware.getHwSerialNum() == null || hardware.getHwSerialNum().trim().isEmpty()) {
            throw new HardwareInvalidoException("El número de serie es obligatorio");
        }
        
        if (hardware.getModelo() == null) {
            throw new HardwareInvalidoException("El modelo es obligatorio");
        }
        
        if (hardware.getActivo() == null) {
            throw new HardwareInvalidoException("El activo asociado es obligatorio");
        }
        
        // Validar que el valor de compra sea positivo
        if (hardware.getHwValorCompra() != null && 
            hardware.getHwValorCompra().compareTo(BigDecimal.ZERO) < 0) {
            throw new HardwareInvalidoException(
                "El valor de compra no puede ser negativo"
            );
        }
    }

    // ==================== EXCEPCIONES PERSONALIZADAS ====================

    public static class HardwareNoEncontradoException extends RuntimeException {
        public HardwareNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class HardwareInvalidoException extends RuntimeException {
        public HardwareInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SerialDuplicadoException extends RuntimeException {
        public SerialDuplicadoException(String mensaje) {
            super(mensaje);
        }
    }
}