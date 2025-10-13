package com.sistema.iTsystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.CategoriasActivo;
import com.sistema.iTsystem.model.Departamentos;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.CategoriaEstadoRepository;
import com.sistema.iTsystem.repository.DepartamentosRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;

@Service
public class ActivoService {

    // ==================== EXCEPCIONES PERSONALIZADAS ====================

    public static class ActivoNoEncontradoException extends RuntimeException {
        public ActivoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ActivoInvalidoException extends RuntimeException {
        public ActivoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ActivoConDependenciasException extends RuntimeException {
        public ActivoConDependenciasException(String mensaje) {
            super(mensaje);
        }
    }

    // ==================== AUTOWIRED ====================

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private CategoriasActivoRepository categoriasRepository;

    @Autowired
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private DepartamentosRepository departamentosRepository;

    @Autowired
    private CategoriaEstadoRepository categoriaEstadoRepository;

    @Autowired
    private EstadoTransicionService estadoTransicionService;

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los activos
     */
    public List<Activo> obtenerTodos() {
        return activoRepository.findAll();
    }

    /**
     * Obtener todos los activos con paginación
     */
    public Page<Activo> obtenerTodos(Pageable pageable) {
        return activoRepository.findAll(pageable);
    }

    /**
     * Buscar activo por ID
     */
    public Optional<Activo> buscarPorId(Long id) {
        return activoRepository.findById(id);
    }

    /**
     * Crear nuevo activo
     */
    @Transactional
    public Activo crear(Activo activo) {
        validarActivo(activo);
        
        // Asignar estado inicial según categoría
        if (activo.getEstado() == null) {
            activo.setEstado(obtenerEstadoInicial(activo.getCategoria()));
        }
        
        // Asignar fecha de ingreso
        if (activo.getActivoFechaIngreso() == null) {
            activo.setActivoFechaIngreso(LocalDateTime.now());
        }
        
        return activoRepository.save(activo);
    }

    /**
     * Actualizar activo existente (NO cambia estado aquí)
     */
    @Transactional
    public Activo actualizar(Long id, Activo activoActualizado) {
        Activo activoExistente = activoRepository.findById(id)
            .orElseThrow(() -> new ActivoNoEncontradoException(
                "Activo con ID " + id + " no encontrado"
            ));
        
        validarActivo(activoActualizado);
        
        // Actualizar solo campos básicos (NO estado)
        activoExistente.setActivoNom(activoActualizado.getActivoNom());
        activoExistente.setCategoria(activoActualizado.getCategoria());
        activoExistente.setDepartamento(activoActualizado.getDepartamento());
        
        return activoRepository.save(activoExistente);
    }

    /**
     * Eliminar activo (solo si está en estado "Baja" o "Cancelado")
     */
    @Transactional
    public void eliminar(Long id) {
        Activo activo = activoRepository.findById(id)
            .orElseThrow(() -> new ActivoNoEncontradoException(
                "Activo con ID " + id + " no encontrado"
            ));
        
        // Validar que esté en estado "Baja" o "Cancelado"
        if (!"Baja".equals(activo.getEstado().getEstadoNom()) && 
            !"Cancelado".equals(activo.getEstado().getEstadoNom())) {
            throw new ActivoConDependenciasException(
                "Solo se pueden eliminar activos en estado 'Baja' o 'Cancelado'"
            );
        }
        
        activoRepository.delete(activo);
    }

    /**
     * Cambiar estado de un activo (delega al EstadoTransicionService)
     */
    @Transactional
    public void cambiarEstado(Long activoId, Long nuevoEstadoId, String motivo, 
                             String observaciones, Usuario usuario) {
        estadoTransicionService.cambiarEstado(activoId, nuevoEstadoId, motivo, observaciones, usuario);
    }

    // ==================== BÚSQUEDAS CON PAGINACIÓN ====================

    /**
     * Buscar activos con filtros combinados y paginación
     */
    public Page<Activo> buscarConFiltros(String buscar, Long categoriaId, Long estadoId, 
                                         Long departamentoId, Pageable pageable) {
        return activoRepository.findByFiltros(buscar, categoriaId, estadoId, departamentoId, pageable);
    }

    /**
     * Buscar por nombre (con paginación)
     */
    public Page<Activo> buscarPorNombre(String nombre, Pageable pageable) {
        return activoRepository.findByActivoNomContainingIgnoreCase(nombre, pageable);
    }

    /**
     * Buscar por categoría (con paginación)
     */
    public Page<Activo> buscarPorCategoria(Long categoriaId, Pageable pageable) {
        return activoRepository.findByCategoria_CatId(categoriaId, pageable);
    }

    /**
     * Buscar por estado (con paginación)
     */
    public Page<Activo> buscarPorEstado(Long estadoId, Pageable pageable) {
        return activoRepository.findByEstado_EstadoId(estadoId, pageable);
    }

    /**
     * Buscar por departamento (con paginación)
     */
    public Page<Activo> buscarPorDepartamento(Long departamentoId, Pageable pageable) {
        return activoRepository.findByDepartamento_DeptId(departamentoId, pageable);
    }

    // ==================== BÚSQUEDAS SIN PAGINACIÓN ====================

    /**
     * Buscar por departamento (objeto completo)
     */
    public List<Activo> buscarPorDepartamento(Departamentos departamento) {
        return activoRepository.findByDepartamento(departamento);
    }

    /**
     * Buscar por estado (objeto completo)
     */
    public List<Activo> buscarPorEstado(EstadoActivo estado) {
        return activoRepository.findByEstado(estado);
    }

    /**
     * Buscar por categoría (objeto completo)
     */
    public List<Activo> buscarPorCategoria(CategoriasActivo categoria) {
        return activoRepository.findByCategoria(categoria);
    }

    // ==================== CONTADORES ====================

    /**
     * Contar activos por estado
     */
    public long contarPorEstado(EstadoActivo estado) {
        return activoRepository.countByEstado(estado);
    }

    /**
     * Contar activos por categoría
     */
    public long contarPorCategoria(CategoriasActivo categoria) {
        return activoRepository.countByCategoria(categoria);
    }

    /**
     * Contar activos por departamento
     */
    public long contarPorDepartamento(Departamentos departamento) {
        return activoRepository.countByDepartamento(departamento);
    }

    /**
     * Contar total de activos
     */
    public long contarTodos() {
        return activoRepository.count();
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Contar activos por estado (para dashboard)
     */
    public List<Object[]> contarPorEstado() {
        return activoRepository.countActivosPorEstado();
    }

    /**
     * Contar activos por categoría (para dashboard)
     */
    public List<Object[]> contarPorCategoria() {
        return activoRepository.countActivosPorCategoria();
    }

    /**
     * Contar activos por departamento (para dashboard)
     */
    public List<Object[]> contarPorDepartamento() {
        return activoRepository.countActivosPorDepartamento();
    }

    // ==================== GESTIÓN DE ESTADOS POR CATEGORÍA ====================

    /**
     * Obtener estados aplicables según categoría
     */
    public List<EstadoActivo> obtenerEstadosPorCategoria(Long categoriaId) {
        return categoriaEstadoRepository.findEstadosByCategoriaId(categoriaId);
    }

    /**
     * Validar que un estado sea aplicable a una categoría
     */
    public boolean esEstadoValidoParaCategoria(Long categoriaId, Long estadoId) {
        return categoriaEstadoRepository.existsByCategoriaIdAndEstadoId(categoriaId, estadoId);
    }

    /**
     * Obtener estado inicial según categoría
     */
    private EstadoActivo obtenerEstadoInicial(CategoriasActivo categoria) {
        String nombreCategoria = categoria.getCatNom().toLowerCase();
        String estadoInicial;
        
        if (nombreCategoria.contains("hardware")) {
            estadoInicial = "Proyectado";
        } else if (nombreCategoria.contains("software")) {
            estadoInicial = "Activo";
        } else if (nombreCategoria.contains("servicio")) {
            estadoInicial = "Activo";
        } else if (nombreCategoria.contains("contrato")) {
            estadoInicial = "Vigente";
        } else {
            estadoInicial = "Activo"; // Default
        }
        
        return estadoActivoRepository.findByEstadoNom(estadoInicial)
            .orElseThrow(() -> new ActivoInvalidoException(
                "Estado inicial '" + estadoInicial + "' no encontrado"
            ));
    }

    // ==================== VALIDACIONES ====================

    /**
     * Validar datos de activo
     */
    private void validarActivo(Activo activo) {
        if (activo.getActivoNom() == null || activo.getActivoNom().trim().isEmpty()) {
            throw new ActivoInvalidoException("El nombre del activo es obligatorio");
        }
        
        if (activo.getCategoria() == null) {
            throw new ActivoInvalidoException("La categoría es obligatoria");
        }
        
        if (activo.getDepartamento() == null) {
            throw new ActivoInvalidoException("El departamento es obligatorio");
        }
        
        //Validar que el estado sea compatible con la categoría
        if (activo.getEstado() != null) {
            boolean esValido = esEstadoValidoParaCategoria(
                activo.getCategoria().getCatId(), 
                activo.getEstado().getEstadoId()
            );
            
            if (!esValido) {
                throw new ActivoInvalidoException(
                    "El estado '" + activo.getEstado().getEstadoNom() + 
                    "' no es válido para la categoría '" + activo.getCategoria().getCatNom() + "'"
                );
            }
        }
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    /**
     * Obtener todas las categorías
     */
    public List<CategoriasActivo> obtenerTodasCategorias() {
        return categoriasRepository.findAllByOrderByCatNomAsc();
    }

    /**
     * Obtener todos los estados
     */
    public List<EstadoActivo> obtenerTodosEstados() {
        return estadoActivoRepository.findAllByOrderByEstadoNomAsc();
    }

    /**
     * Obtener todos los departamentos
     */
    public List<Departamentos> obtenerTodosDepartamentos() {
        return departamentosRepository.findAll();
    }
}