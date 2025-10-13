package com.sistema.iTsystem.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sistema.iTsystem.model.LicenciaInfo;
import com.sistema.iTsystem.model.Proveedores;
import com.sistema.iTsystem.model.SoftTipo;
import com.sistema.iTsystem.model.SoftwareInfo;
import com.sistema.iTsystem.repository.LicenciaInfoRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;
import com.sistema.iTsystem.repository.SoftTipoRepository;
import com.sistema.iTsystem.repository.SoftwareInfoRepository;

@Service
public class SoftwareInfoService {

    @Autowired
    private SoftwareInfoRepository softwareRepository;
    
    @Autowired
    private SoftTipoRepository softTipoRepository;
    
    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private LicenciaInfoRepository licenciaRepository; 

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los software
     */
    public List<SoftwareInfo> obtenerTodos() {
        return softwareRepository.findAll();
    }

    /**
     * Buscar software por ID
     */
    public Optional<SoftwareInfo> buscarPorId(Long id) {
        return softwareRepository.findById(id);
    }

    /**
     * Buscar software por ID con detalles (JOIN FETCH)
     */
    public Optional<SoftwareInfo> buscarPorIdConDetalles(Long id) {
        return softwareRepository.findByIdWithDetails(id);
    }

    /**
     * Buscar software por activo ID
     */
    public Optional<SoftwareInfo> buscarPorActivoId(Long activoId) {
        return softwareRepository.findByActivo_ActivoId(activoId);
    }

    /**
     * Crear nuevo software
     */
    @Transactional
    public SoftwareInfo crear(SoftwareInfo software) {
        validarSoftware(software);
        return softwareRepository.save(software);
    }

    /**
     * Actualizar software existente
     */
    @Transactional
    public SoftwareInfo actualizar(Long id, SoftwareInfo softwareActualizado) {
        SoftwareInfo softwareExistente = softwareRepository.findById(id)
            .orElseThrow(() -> new SoftwareNoEncontradoException(
                "Software con ID " + id + " no encontrado"
            ));
        
        validarSoftware(softwareActualizado);
        
        // Actualizar campos
        softwareExistente.setSftNom(softwareActualizado.getSftNom());
        softwareExistente.setSftVersion(softwareActualizado.getSftVersion());
        softwareExistente.setSoftTipo(softwareActualizado.getSoftTipo());
        softwareExistente.setProveedor(softwareActualizado.getProveedor());
        
        return softwareRepository.save(softwareExistente);
    }

    /**
     * Eliminar software
     */
    @Transactional
    public void eliminar(Long id) {
        SoftwareInfo software = softwareRepository.findById(id)
            .orElseThrow(() -> new SoftwareNoEncontradoException(
                "Software con ID " + id + " no encontrado"
            ));
        
        // Validar que no tenga licencias asociadas
        if (tieneLicenciasAsociadas(software)) {
            throw new SoftwareConDependenciasException(
                "No se puede eliminar el software porque tiene licencias asociadas"
            );
        }
        
        softwareRepository.delete(software);
    }

    // ==================== BÚSQUEDAS AVANZADAS ====================

    /**
     * Buscar software por nombre
     */
    public List<SoftwareInfo> buscarPorNombre(String nombre) {
        return softwareRepository.findBySftNomContainingIgnoreCase(nombre);
    }

    /**
     * Buscar software por tipo
     */
    public List<SoftwareInfo> buscarPorTipo(Long tipoId) {
        return softwareRepository.findBySoftTipo_SoftTipoId(tipoId);
    }

    /**
     * Buscar software por proveedor
     */
    public List<SoftwareInfo> buscarPorProveedor(Long provId) {
        return softwareRepository.findByProveedor_ProvId(provId);
    }

    /**
     * Buscar software sin proveedor asignado
     */
    public List<SoftwareInfo> buscarSinProveedor() {
        return softwareRepository.findSoftwareSinProveedor();
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Contar software por tipo
     */
    public List<Object[]> contarPorTipo() {
        return softwareRepository.countSoftwarePorTipo();
    }

    /**
     * Contar software por proveedor
     */
    public List<Object[]> contarPorProveedor() {
        return softwareRepository.countSoftwarePorProveedor();
    }

    // ==================== VERIFICACIONES ====================

    /**
     * Verificar si el software tiene licencias asociadas
     */
    public boolean tieneLicenciasAsociadas(SoftwareInfo software) {
        List<LicenciaInfo> licencias = licenciaRepository.findBySoftwareInfo_SftId(software.getSftId());
        return !licencias.isEmpty();
    }

    /**
     * Verificar si el software tiene licencias activas
     */
    public boolean tieneLicenciasActivas(Long softwareId) {
        List<LicenciaInfo> licenciasActivas = licenciaRepository.findLicenciasActivas(LocalDate.now());
        return licenciasActivas.stream()
        .anyMatch(l -> l.getSoftwareInfo().getSftId().equals(softwareId));
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    /**
     * Obtener todos los tipos de software
     */
    public List<SoftTipo> obtenerTodosTipos() {
        return softTipoRepository.findAll();
    }

    /**
     * Obtener todos los proveedores
     */
    public List<Proveedores> obtenerTodosProveedores() {
        return proveedoresRepository.findAll();
    }

    // ==================== VALIDACIONES ====================

    /**
     * Validar datos de software
     */
    private void validarSoftware(SoftwareInfo software) {
        if (software.getSftNom() == null || software.getSftNom().trim().isEmpty()) {
            throw new SoftwareInvalidoException("El nombre del software es obligatorio");
        }
        
        if (software.getSoftTipo() == null) {
            throw new SoftwareInvalidoException("El tipo de software es obligatorio");
        }
        
        if (software.getActivo() == null) {
            throw new SoftwareInvalidoException("El activo asociado es obligatorio");
        }
    }

    // ==================== EXCEPCIONES PERSONALIZADAS ====================

    public static class SoftwareNoEncontradoException extends RuntimeException {
        public SoftwareNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SoftwareInvalidoException extends RuntimeException {
        public SoftwareInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SoftwareConDependenciasException extends RuntimeException {
        public SoftwareConDependenciasException(String mensaje) {
            super(mensaje);
        }
    }
}