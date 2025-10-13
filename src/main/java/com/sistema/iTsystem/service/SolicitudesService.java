package com.sistema.iTsystem.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.SoliEstados;
import com.sistema.iTsystem.model.SoliTipos;
import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.SoliEstadosRepository;
import com.sistema.iTsystem.repository.SoliTiposRepository;
import com.sistema.iTsystem.repository.SolicitudesRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;

@Service
public class SolicitudesService {

    // ==================== EXCEPCIONES PERSONALIZADAS (AL INICIO) ====================

    public static class SolicitudNoEncontradaException extends RuntimeException {
        public SolicitudNoEncontradaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SolicitudInvalidaException extends RuntimeException {
        public SolicitudInvalidaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class CambioEstadoInvalidoException extends RuntimeException {
        public CambioEstadoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    // ==================== AUTOWIRED ====================

    @Autowired
    private SolicitudesRepository solicitudesRepository;
    
    @Autowired
    private SoliEstadosRepository estadosRepository;
    
    @Autowired
    private SoliTiposRepository tiposRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todas las solicitudes
     */
    public List<Solicitudes> obtenerTodas() {
        return solicitudesRepository.findAll();
    }

    /**
     * Obtener todas las solicitudes con paginación
     */
    public Page<Solicitudes> obtenerTodas(Pageable pageable) {
        return solicitudesRepository.findAll(pageable);
    }

    /**
     * Buscar solicitud por ID
     */
    public Optional<Solicitudes> buscarPorId(Long id) {
        return solicitudesRepository.findById(id);
    }

    /**
     * Buscar solicitud por ID con detalles (JOIN FETCH)
     */
    public Solicitudes buscarPorIdConDetalles(Long id) {
        return solicitudesRepository.findByIdWithDetails(id);
    }

    /**
     * Crear nueva solicitud
     */
    @Transactional
    public Solicitudes crear(Solicitudes solicitud, Usuario usuario) {
        validarSolicitud(solicitud);
        
        // Asignar usuario que crea la solicitud
        solicitud.setUsuario(usuario);
        
        // Asignar estado "Pendiente" por defecto
        if (solicitud.getSoliEstado() == null) {
            SoliEstados estadoPendiente = estadosRepository.findBySoliEstadoNom("Pendiente")
                .orElseThrow(() -> new SolicitudInvalidaException(
                    "Estado 'Pendiente' no encontrado en la base de datos"
                ));
            solicitud.setSoliEstado(estadoPendiente);
        }
        
        return solicitudesRepository.save(solicitud);
    }

    /**
     * Actualizar solicitud existente
     */
    @Transactional
    public Solicitudes actualizar(Long id, Solicitudes solicitudActualizada) {
        Solicitudes solicitudExistente = solicitudesRepository.findById(id)
            .orElseThrow(() -> new SolicitudNoEncontradaException(
                "Solicitud con ID " + id + " no encontrada"
            ));
        
        validarSolicitud(solicitudActualizada);
        
        // Actualizar campos (NO se actualiza usuario ni estado aquí)
        solicitudExistente.setSoliDescri(solicitudActualizada.getSoliDescri());
        solicitudExistente.setSoliMotivo(solicitudActualizada.getSoliMotivo());
        solicitudExistente.setSoliTipo(solicitudActualizada.getSoliTipo());
        
        return solicitudesRepository.save(solicitudExistente);
    }

    /**
     * Eliminar solicitud
     */
    @Transactional
    public void eliminar(Long id) {
        Solicitudes solicitud = solicitudesRepository.findById(id)
            .orElseThrow(() -> new SolicitudNoEncontradaException(
                "Solicitud con ID " + id + " no encontrada"
            ));
        
        solicitudesRepository.delete(solicitud);
    }

    // ==================== GESTIÓN DE ESTADOS ====================

    /**
     * Cambiar estado de una solicitud
     */
    @Transactional
    public Solicitudes cambiarEstado(Long solicitudId, Long nuevoEstadoId) {
        Solicitudes solicitud = solicitudesRepository.findById(solicitudId)
            .orElseThrow(() -> new SolicitudNoEncontradaException(
                "Solicitud con ID " + solicitudId + " no encontrada"
            ));
        
        SoliEstados nuevoEstado = estadosRepository.findById(nuevoEstadoId)
            .orElseThrow(() -> new SolicitudInvalidaException(
                "Estado con ID " + nuevoEstadoId + " no encontrado"
            ));
        
        solicitud.setSoliEstado(nuevoEstado);
        return solicitudesRepository.save(solicitud);
    }

    /**
     * Aprobar solicitud
     */
    @Transactional
    public Solicitudes aprobar(Long solicitudId) {
        Solicitudes solicitud = solicitudesRepository.findById(solicitudId)
            .orElseThrow(() -> new SolicitudNoEncontradaException(
                "Solicitud con ID " + solicitudId + " no encontrada"
            ));
        
        // Validar que esté pendiente
        if (!solicitud.estaPendiente()) {
            throw new CambioEstadoInvalidoException(
                "Solo se pueden aprobar solicitudes pendientes"
            );
        }
        
        SoliEstados estadoAprobada = estadosRepository.findBySoliEstadoNom("Aprobada")
            .orElseThrow(() -> new SolicitudInvalidaException(
                "Estado 'Aprobada' no encontrado"
            ));
        
        solicitud.setSoliEstado(estadoAprobada);
        return solicitudesRepository.save(solicitud);
    }

    /**
     * Rechazar solicitud
     */
    @Transactional
    public Solicitudes rechazar(Long solicitudId, String motivo) {
        Solicitudes solicitud = solicitudesRepository.findById(solicitudId)
            .orElseThrow(() -> new SolicitudNoEncontradaException(
                "Solicitud con ID " + solicitudId + " no encontrada"
            ));
        
        // Validar que esté pendiente
        if (!solicitud.estaPendiente()) {
            throw new CambioEstadoInvalidoException(
                "Solo se pueden rechazar solicitudes pendientes"
            );
        }
        
        SoliEstados estadoRechazada = estadosRepository.findBySoliEstadoNom("Rechazada")
            .orElseThrow(() -> new SolicitudInvalidaException(
                "Estado 'Rechazada' no encontrado"
            ));
        
        solicitud.setSoliEstado(estadoRechazada);
        
        // Agregar motivo de rechazo a la descripción
        if (motivo != null && !motivo.trim().isEmpty()) {
            String descripcionActual = solicitud.getSoliDescri() != null ? 
                solicitud.getSoliDescri() : "";
            solicitud.setSoliDescri(descripcionActual + "\n[RECHAZADA] Motivo: " + motivo);
        }
        
        return solicitudesRepository.save(solicitud);
    }

    /**
     * Completar solicitud
     */
    @Transactional
    public Solicitudes completar(Long solicitudId) {
        Solicitudes solicitud = solicitudesRepository.findById(solicitudId)
            .orElseThrow(() -> new SolicitudNoEncontradaException(
                "Solicitud con ID " + solicitudId + " no encontrada"
            ));
        
        // Validar que esté aprobada
        if (!solicitud.estaAprobada()) {
            throw new CambioEstadoInvalidoException(
                "Solo se pueden completar solicitudes aprobadas"
            );
        }
        
        SoliEstados estadoCompletada = estadosRepository.findBySoliEstadoNom("Completada")
            .orElseThrow(() -> new SolicitudInvalidaException(
                "Estado 'Completada' no encontrado"
            ));
        
        solicitud.setSoliEstado(estadoCompletada);
        return solicitudesRepository.save(solicitud);
    }

    // ==================== BÚSQUEDAS POR USUARIO ====================

    /**
     * Buscar solicitudes por usuario
     */
    public List<Solicitudes> buscarPorUsuario(Long usuarioId) {
        return solicitudesRepository.findByUsuario_UsuIdOrderByCreatedAtDesc(usuarioId);
    }

    /**
     * Buscar solicitudes por usuario con paginación
     */
    public Page<Solicitudes> buscarPorUsuario(Long usuarioId, Pageable pageable) {
        return solicitudesRepository.findByUsuario_UsuId(usuarioId, pageable);
    }

    // ==================== BÚSQUEDAS POR ESTADO ====================

    /**
     * Buscar solicitudes por estado
     */
    public List<Solicitudes> buscarPorEstado(Long estadoId) {
        return solicitudesRepository.findBySoliEstado_SoliEstadoId(estadoId);
    }

    /**
     * Buscar solicitudes por estado con paginación
     */
    public Page<Solicitudes> buscarPorEstado(Long estadoId, Pageable pageable) {
        return solicitudesRepository.findBySoliEstado_SoliEstadoId(estadoId, pageable);
    }

    /**
     * Buscar solicitudes pendientes
     */
    public List<Solicitudes> buscarPendientes() {
        return solicitudesRepository.findSolicitudesPendientes();
    }

    /**
     * Buscar solicitudes aprobadas
     */
    public List<Solicitudes> buscarAprobadas() {
        return solicitudesRepository.findSolicitudesAprobadas();
    }

    /**
     * Buscar solicitudes rechazadas
     */
    public List<Solicitudes> buscarRechazadas() {
        return solicitudesRepository.findSolicitudesRechazadas();
    }

    // ==================== BÚSQUEDAS POR TIPO ====================

    /**
     * Buscar solicitudes por tipo
     */
    public List<Solicitudes> buscarPorTipo(Long tipoId) {
        return solicitudesRepository.findBySoliTipo_SoliTipoId(tipoId);
    }

    /**
     * Buscar solicitudes por tipo con paginación
     */
    public Page<Solicitudes> buscarPorTipo(Long tipoId, Pageable pageable) {
        return solicitudesRepository.findBySoliTipo_SoliTipoId(tipoId, pageable);
    }

    // ==================== BÚSQUEDAS POR FECHA ====================

    /**
     * Buscar solicitudes por rango de fechas
     */
    public List<Solicitudes> buscarPorFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return solicitudesRepository.findByFechaBetween(fechaInicio, fechaFin);
    }

    // ==================== BÚSQUEDAS POR DESCRIPCIÓN ====================

    /**
     * Buscar solicitudes por descripción
     */
    public List<Solicitudes> buscarPorDescripcion(String texto) {
        return solicitudesRepository.findBySoliDescriContaining(texto);
    }

    // ==================== BÚSQUEDAS COMBINADAS ====================

    /**
     * Buscar solicitudes por usuario y estado
     */
    public List<Solicitudes> buscarPorUsuarioYEstado(Long usuarioId, Long estadoId) {
        return solicitudesRepository.findByUsuario_UsuIdAndSoliEstado_SoliEstadoId(usuarioId, estadoId);
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Contar solicitudes por estado
     */
    public List<Object[]> contarPorEstado() {
        return solicitudesRepository.countSolicitudesPorEstado();
    }

    /**
     * Contar solicitudes por tipo
     */
    public List<Object[]> contarPorTipo() {
        return solicitudesRepository.countSolicitudesPorTipo();
    }

    /**
     * Contar solicitudes por usuario
     */
    public List<Object[]> contarPorUsuario() {
        return solicitudesRepository.countSolicitudesPorUsuario();
    }

    /**
     * Contar solicitudes pendientes
     */
    public Long contarPendientes() {
        return solicitudesRepository.countSolicitudesPendientes();
    }

    /**
     * Obtener últimas solicitudes
     */
    public List<Solicitudes> obtenerUltimas(int cantidad) {
        return solicitudesRepository.findTop10ByOrderByCreatedAtDesc();
    }

    // ==================== VALIDACIONES ====================

    /**
     * Validar datos de solicitud
     */
    private void validarSolicitud(Solicitudes solicitud) {
        if (solicitud.getSoliTipo() == null) {
            throw new SolicitudInvalidaException("El tipo de solicitud es obligatorio");
        }
        
        if (solicitud.getSoliDescri() == null || solicitud.getSoliDescri().trim().isEmpty()) {
            throw new SolicitudInvalidaException("La descripción de la solicitud es obligatoria");
        }
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    /**
     * Obtener todos los estados de solicitudes
     */
    public List<SoliEstados> obtenerTodosEstados() {
        return estadosRepository.findAll();
    }

    /**
     * Obtener todos los tipos de solicitudes
     */
    public List<SoliTipos> obtenerTodosTipos() {
        return tiposRepository.findAll();
    }

    /**
     * Obtener todos los usuarios
     */
    public List<Usuario> obtenerTodosUsuarios() {
        return usuarioRepository.findAll();
    }
}