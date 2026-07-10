package com.sistema.iTsystem.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.dto.solicitudes.SolicitudCambioEstadoDTO;
import com.sistema.iTsystem.dto.solicitudes.SolicitudFormDTO;
import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.SoliEstados;
import com.sistema.iTsystem.model.SoliTipos;
import com.sistema.iTsystem.model.SolicitudHistorialEstados;
import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.model.Marca;
import com.sistema.iTsystem.model.Modelo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.MarcaRepository;
import com.sistema.iTsystem.repository.ModeloRepository;
import com.sistema.iTsystem.repository.SolicitudHistorialEstadosRepository;
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

    @Autowired
    private SolicitudHistorialEstadosRepository historialEstadosRepository;

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ModeloRepository modeloRepository;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
        "Mantenimiento",
        "Baja",
        "Compra",
        "Asignación"
    );

    private static final Set<String> ESTADOS_PERMITIDOS = Set.of(
        "Pendiente",
        "En análisis",
        "Aprobada",
        "Rechazada",
        "Cerrada",
        "Cancelada"
    );

    private static final Map<String, Set<String>> TRANSICIONES_PERMITIDAS = new HashMap<>();

    static {
        TRANSICIONES_PERMITIDAS.put("Pendiente", Set.of("En análisis", "Rechazada", "Cancelada"));
        TRANSICIONES_PERMITIDAS.put("En análisis", Set.of("Aprobada", "Rechazada", "Cancelada"));
        TRANSICIONES_PERMITIDAS.put("Aprobada", Set.of("Cerrada"));
        TRANSICIONES_PERMITIDAS.put("Rechazada", Set.of());
        TRANSICIONES_PERMITIDAS.put("Cerrada", Set.of());
        TRANSICIONES_PERMITIDAS.put("Cancelada", Set.of());
    }

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

    public List<SoliEstados> obtenerEstadosPermitidos() {
        return estadosRepository.findAll().stream()
            .filter(estado -> estado.getSoliEstadoNom() != null
                && ESTADOS_PERMITIDOS.contains(estado.getSoliEstadoNom()))
            .sorted(Comparator.comparing(SoliEstados::getSoliEstadoNom))
            .toList();
    }

    public List<SoliTipos> obtenerTiposPermitidos() {
        return tiposRepository.findAll().stream()
            .filter(tipo -> tipo.getSoliTipoNom() != null
                && TIPOS_PERMITIDOS.contains(tipo.getSoliTipoNom()))
            .sorted(Comparator.comparing(SoliTipos::getSoliTipoNom))
            .toList();
    }

    public List<Usuario> obtenerUsuariosActivos() {
        return usuarioRepository.findAll().stream()
            .filter(usuario -> Boolean.TRUE.equals(usuario.getUsuActivo()))
            .sorted(Comparator.comparing(usuario -> usuario.getUsuLogin() != null ? usuario.getUsuLogin().toLowerCase() : ""))
            .toList();
    }

    public List<Usuario> obtenerResponsablesDisponibles() {
        return obtenerUsuariosActivos().stream()
            .filter(usuario -> esSuperAdmin(usuario) || esTecnico(usuario))
            .toList();
    }

    public List<Activo> obtenerActivosReferenciales() {
        return activoRepository.findAll().stream()
            .filter(activo -> Boolean.TRUE.equals(activo.getActivoActivo()))
            .sorted(Comparator.comparing(activo -> activo.getActivoCodigo() != null ? activo.getActivoCodigo().toLowerCase() : ""))
            .toList();
    }

    public List<Marca> obtenerMarcasActivas() {
        return marcaRepository.findAll().stream()
            .filter(marca -> Boolean.TRUE.equals(marca.getMarcaActiva()))
            .sorted(Comparator.comparing(marca -> marca.getMarcaNom() != null ? marca.getMarcaNom().toLowerCase() : ""))
            .toList();
    }

    public List<Modelo> obtenerModelosActivos() {
        return modeloRepository.findAll().stream()
            .filter(modelo -> Boolean.TRUE.equals(modelo.getModelActivo()))
            .sorted(Comparator.comparing(modelo -> modelo.getModelNom() != null ? modelo.getModelNom().toLowerCase() : ""))
            .toList();
    }

    public Long obtenerEstadoIdPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }

        return estadosRepository.findBySoliEstadoNom(nombre)
            .map(SoliEstados::getSoliEstadoId)
            .orElse(null);
    }

    public Long obtenerTipoIdPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }

        return tiposRepository.findBySoliTipoNom(nombre)
            .map(SoliTipos::getSoliTipoId)
            .orElse(null);
    }

    public List<SolicitudHistorialEstados> obtenerHistorial(Long solicitudId) {
        return historialEstadosRepository.findBySolicitudIdWithDetails(solicitudId);
    }

    public Page<Solicitudes> buscarConFiltros(String texto, Long tipoId, Long estadoId,
                                              Long solicitanteId, Long responsableId,
                                              LocalDate fechaDesde, LocalDate fechaHasta,
                                              Usuario usuarioActual, Pageable pageable) {
        Long scopeUsuarioId = null;
        Long scopeResponsableId = null;

        if (usuarioActual != null) {
            if (esStandardUser(usuarioActual)) {
                scopeUsuarioId = usuarioActual.getUsuId();
            } else if (esTecnico(usuarioActual)) {
                scopeUsuarioId = usuarioActual.getUsuId();
                scopeResponsableId = usuarioActual.getUsuId();
            }
        }

        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : null;

        return solicitudesRepository.findWithFilters(
            texto,
            tipoId,
            estadoId,
            solicitanteId,
            responsableId,
            desde,
            hasta,
            scopeUsuarioId,
            scopeResponsableId,
            pageable
        );
    }

    @Transactional
    public Solicitudes crearDesdeFormulario(SolicitudFormDTO form, Usuario usuarioAutenticado) {
        SolicitudFormDTO formulario = form != null ? form : new SolicitudFormDTO();
        validarFormularioSolicitud(formulario, false);

        SoliTipos tipo = obtenerTipoObligatorio(formulario.getTipoId());
        Solicitudes solicitud = new Solicitudes();
        solicitud.setSoliTipo(tipo);
        solicitud.setSoliMotivo(normalizarTexto(formulario.getSoliMotivo()));
        solicitud.setSoliDescri(normalizarTexto(formulario.getSoliDescri()));
        solicitud.setUsuario(usuarioAutenticado);
        solicitud.setSoliEstado(obtenerEstadoObligatorio("Pendiente"));

        aplicarCamposEspecificosPorTipo(solicitud, formulario, tipo);
        solicitud = solicitudesRepository.save(solicitud);

        registrarHistorial(solicitud, null, solicitud.getSoliEstado(), usuarioAutenticado, "Creación de solicitud");
        return solicitud;
    }

    @Transactional
    public Solicitudes actualizarDesdeFormulario(Long id, SolicitudFormDTO form, Usuario usuarioAutenticado) {
        Solicitudes solicitud = solicitudesRepository.findById(id)
            .orElseThrow(() -> new SolicitudNoEncontradaException("Solicitud con ID " + id + " no encontrada"));

        validarEdicionPermitida(solicitud, usuarioAutenticado);
        validarFormularioSolicitud(form, false);

        SoliTipos tipo = obtenerTipoObligatorio(form.getTipoId());
        solicitud.setSoliTipo(tipo);
        solicitud.setSoliMotivo(normalizarTexto(form.getSoliMotivo()));
        solicitud.setSoliDescri(normalizarTexto(form.getSoliDescri()));
        aplicarCamposEspecificosPorTipo(solicitud, form, tipo);

        return solicitudesRepository.save(solicitud);
    }

    @Transactional
    public Solicitudes cambiarEstado(Long solicitudId, SolicitudCambioEstadoDTO dto, Usuario usuarioAutenticado) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }

        if (dto == null || dto.getNuevoEstadoId() == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un estado destino");
        }

        SoliEstados nuevoEstado = obtenerEstadoPorId(dto.getNuevoEstadoId());
        String nombreNuevoEstado = nuevoEstado.getSoliEstadoNom();
        String nombreEstadoActual = solicitud.getSoliEstado() != null ? solicitud.getSoliEstado().getSoliEstadoNom() : null;

        validarPermisoCambioEstado(solicitud, nombreNuevoEstado, usuarioAutenticado);
        validarTransicion(nombreEstadoActual, nombreNuevoEstado);

        Usuario responsable = solicitud.getResponsable();
        if ("En análisis".equalsIgnoreCase(nombreNuevoEstado)) {
            Long responsableId = dto.getResponsableId() != null ? dto.getResponsableId() : (responsable != null ? responsable.getUsuId() : null);
            responsable = validarResponsable(responsableId);
            if (responsable == null) {
                throw new SolicitudInvalidaException("Debe asignar un responsable antes de pasar la solicitud a En análisis.");
            }
        } else if (dto.getResponsableId() != null) {
            responsable = validarResponsable(dto.getResponsableId());
        }

        solicitud.setSoliEstado(nuevoEstado);
        solicitud.setResponsable(responsable);
        solicitudesRepository.save(solicitud);

        registrarHistorial(solicitud, nombreEstadoActual, nuevoEstado, usuarioAutenticado,
            dto.getObservacion() != null ? dto.getObservacion() : "Cambio de estado");
        return solicitud;
    }

    @Transactional
    public Solicitudes asignarResponsable(Long solicitudId, Long responsableId, Usuario usuarioAutenticado) {
        Solicitudes solicitud = solicitudesRepository.findById(solicitudId)
            .orElseThrow(() -> new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada"));

        validarPermisoAsignarResponsable(usuarioAutenticado);
        Usuario responsable = validarResponsable(responsableId);
        if (responsable == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un responsable válido");
        }

        solicitud.setResponsable(responsable);
        return solicitudesRepository.save(solicitud);
    }

    @Transactional
    public Solicitudes aprobar(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        return cambiarEstado(solicitudId, crearCambioEstado("Aprobada", null, observacion), usuarioAutenticado);
    }

    @Transactional
    public Solicitudes rechazar(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        return cambiarEstado(solicitudId, crearCambioEstado("Rechazada", null, observacion), usuarioAutenticado);
    }

    @Transactional
    public Solicitudes cerrar(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        return cambiarEstado(solicitudId, crearCambioEstado("Cerrada", null, observacion), usuarioAutenticado);
    }

    @Transactional
    public Solicitudes cancelar(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        return cambiarEstado(solicitudId, crearCambioEstado("Cancelada", null, observacion), usuarioAutenticado);
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

    private void validarFormularioSolicitud(SolicitudFormDTO form, boolean permitirCantidadNula) {
        if (form == null) {
            throw new SolicitudInvalidaException("Debe completar el formulario de solicitud");
        }
        if (form.getTipoId() == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un tipo de solicitud.");
        }
        if (form.getSoliMotivo() == null || form.getSoliMotivo().trim().isEmpty()) {
            throw new SolicitudInvalidaException("El motivo es obligatorio.");
        }
        if (form.getSoliDescri() == null || form.getSoliDescri().trim().isEmpty()) {
            throw new SolicitudInvalidaException("La descripción es obligatoria.");
        }
    }

    private void aplicarCamposEspecificosPorTipo(Solicitudes solicitud, SolicitudFormDTO form, SoliTipos tipo) {
        String nombreTipo = tipo.getSoliTipoNom();

        if ("Mantenimiento".equalsIgnoreCase(nombreTipo) || "Baja".equalsIgnoreCase(nombreTipo)) {
            if (form.getActivoId() == null) {
                throw new SolicitudInvalidaException("Debe seleccionar un activo.");
            }
            Activo activo = activoRepository.findById(form.getActivoId())
                .orElseThrow(() -> new SolicitudInvalidaException("El activo seleccionado no existe."));
            if (!Boolean.TRUE.equals(activo.getActivoActivo())) {
                throw new SolicitudInvalidaException("El activo seleccionado ya está dado de baja.");
            }
            if (activo.getEstado() != null && "Dado de baja".equalsIgnoreCase(activo.getEstado().getEstadoNom())) {
                throw new SolicitudInvalidaException("El activo seleccionado ya está dado de baja.");
            }
            solicitud.setActivo(activo);
            solicitud.setMarca(null);
            solicitud.setModelo(null);
            solicitud.setSoliCantidad(null);
            return;
        }

        if ("Compra".equalsIgnoreCase(nombreTipo)) {
            validarCantidad(form.getSoliCantidad(), true);
            validarMarcaYModelo(form.getMarcaId(), form.getModelId(), false);
            solicitud.setActivo(validarActivoOpcional(form.getActivoId()));
            solicitud.setSoliCantidad(form.getSoliCantidad());
            solicitud.setMarca(validarMarcaOpcional(form.getMarcaId()));
            solicitud.setModelo(validarModeloOpcional(form.getModelId(), solicitud.getMarca()));
            return;
        }

        if ("Asignación".equalsIgnoreCase(nombreTipo)) {
            if (form.getSoliCantidad() != null && form.getSoliCantidad() != 1) {
                throw new SolicitudInvalidaException("La cantidad para una solicitud de asignación debe ser 1.");
            }
            validarMarcaYModelo(form.getMarcaId(), form.getModelId(), true);
            solicitud.setActivo(validarActivoOpcional(form.getActivoId()));
            solicitud.setSoliCantidad(1);
            solicitud.setMarca(validarMarcaOpcional(form.getMarcaId()));
            solicitud.setModelo(validarModeloOpcional(form.getModelId(), solicitud.getMarca()));
            return;
        }

        throw new SolicitudInvalidaException("El tipo seleccionado no está permitido.");
    }

    private void validarCantidad(Integer cantidad, boolean obligatoria) {
        if (cantidad == null) {
            if (obligatoria) {
                throw new SolicitudInvalidaException("La cantidad debe ser mayor que cero.");
            }
            return;
        }
        if (cantidad <= 0) {
            throw new SolicitudInvalidaException("La cantidad debe ser mayor que cero.");
        }
    }

    private void validarMarcaYModelo(Long marcaId, Long modeloId, boolean permitirSinMarca) {
        if (modeloId != null && marcaId == null && !permitirSinMarca) {
            throw new SolicitudInvalidaException("Debe seleccionar una marca antes de elegir un modelo.");
        }
        if (modeloId != null && marcaId == null && permitirSinMarca) {
            throw new SolicitudInvalidaException("Debe seleccionar una marca antes de elegir un modelo.");
        }
    }

    private Activo validarActivoOpcional(Long activoId) {
        if (activoId == null) {
            return null;
        }
        return activoRepository.findById(activoId)
            .orElseThrow(() -> new SolicitudInvalidaException("El activo seleccionado no existe."));
    }

    private Marca validarMarcaOpcional(Long marcaId) {
        if (marcaId == null) {
            return null;
        }
        return marcaRepository.findById(marcaId)
            .orElseThrow(() -> new SolicitudInvalidaException("La marca seleccionada no existe."));
    }

    private Modelo validarModeloOpcional(Long modelId, Marca marca) {
        if (modelId == null) {
            return null;
        }
        Modelo modelo = modeloRepository.findById(modelId)
            .orElseThrow(() -> new SolicitudInvalidaException("El modelo seleccionado no existe."));
        if (marca != null && (modelo.getMarca() == null || !modelo.getMarca().getMarcaId().equals(marca.getMarcaId()))) {
            throw new SolicitudInvalidaException("El modelo seleccionado no pertenece a la marca indicada.");
        }
        return modelo;
    }

    private SoliTipos obtenerTipoObligatorio(Long tipoId) {
        return tiposRepository.findById(tipoId)
            .filter(tipo -> tipo.getSoliTipoNom() != null && TIPOS_PERMITIDOS.contains(tipo.getSoliTipoNom()))
            .orElseThrow(() -> new SolicitudInvalidaException("El tipo seleccionado no está permitido."));
    }

    private SoliEstados obtenerEstadoObligatorio(String nombre) {
        return estadosRepository.findBySoliEstadoNom(nombre)
            .orElseThrow(() -> new SolicitudInvalidaException("Estado '" + nombre + "' no encontrado."));
    }

    private SoliEstados obtenerEstadoPorId(Long estadoId) {
        return estadosRepository.findById(estadoId)
            .orElseThrow(() -> new SolicitudInvalidaException("Estado con ID " + estadoId + " no encontrado."));
    }

    private void validarPermisoCambioEstado(Solicitudes solicitud, String nuevoEstado, Usuario actor) {
        if (actor == null) {
            throw new SolicitudInvalidaException("Debe iniciar sesión nuevamente.");
        }

        if ("En análisis".equalsIgnoreCase(nuevoEstado)) {
            if (esSuperAdmin(actor)) {
                return;
            }
            if (esTecnico(actor) && solicitud.getResponsable() != null && solicitud.getResponsable().getUsuId().equals(actor.getUsuId())) {
                return;
            }
            throw new SolicitudInvalidaException("No tiene permisos para pasar esta solicitud a En análisis.");
        }

        if ("Aprobada".equalsIgnoreCase(nuevoEstado)
                || "Rechazada".equalsIgnoreCase(nuevoEstado)
                || "Cerrada".equalsIgnoreCase(nuevoEstado)) {
            if (!esSuperAdmin(actor)) {
                throw new SolicitudInvalidaException("No tiene permisos para realizar esta transición.");
            }
            return;
        }

        if ("Cancelada".equalsIgnoreCase(nuevoEstado)) {
            if (esSuperAdmin(actor)) {
                return;
            }
            if (esStandardUser(actor)) {
                if (solicitud.getUsuario() == null || !solicitud.getUsuario().getUsuId().equals(actor.getUsuId())) {
                    throw new SolicitudInvalidaException("No tiene permisos para cancelar esta solicitud.");
                }
                if (!solicitud.estaPendiente()) {
                    throw new SolicitudInvalidaException("Solo puede cancelar sus solicitudes pendientes.");
                }
                return;
            }
            if (esTecnico(actor)) {
                throw new SolicitudInvalidaException("No tiene permisos para cancelar esta solicitud.");
            }
            throw new SolicitudInvalidaException("No tiene permisos para cancelar esta solicitud.");
        }
    }

    private void validarTransicion(String estadoActual, String estadoNuevo) {
        if (estadoNuevo == null || estadoNuevo.isBlank()) {
            throw new SolicitudInvalidaException("La transición de estado solicitada no está permitida.");
        }
        if (estadoActual == null) {
            if (!"Pendiente".equalsIgnoreCase(estadoNuevo)) {
                throw new SolicitudInvalidaException("La transición de estado solicitada no está permitida.");
            }
            return;
        }

        Set<String> permitidos = TRANSICIONES_PERMITIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoNuevo)) {
            throw new SolicitudInvalidaException("La transición de estado solicitada no está permitida.");
        }
    }

    private Usuario validarResponsable(Long responsableId) {
        if (responsableId == null) {
            return null;
        }

        Usuario responsable = usuarioRepository.findById(responsableId)
            .orElseThrow(() -> new SolicitudInvalidaException("El responsable seleccionado no existe."));

        if (!Boolean.TRUE.equals(responsable.getUsuActivo())) {
            throw new SolicitudInvalidaException("El responsable debe estar activo.");
        }
        if (!(esSuperAdmin(responsable) || esTecnico(responsable))) {
            throw new SolicitudInvalidaException("El responsable no puede ser un usuario estándar.");
        }
        return responsable;
    }

    private void validarPermisoAsignarResponsable(Usuario actor) {
        if (!(esSuperAdmin(actor) || esTecnico(actor))) {
            throw new SolicitudInvalidaException("No tiene permisos para asignar responsable.");
        }
    }

    private void validarEdicionPermitida(Solicitudes solicitud, Usuario usuarioAutenticado) {
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud no encontrada");
        }
        if (usuarioAutenticado == null) {
            throw new SolicitudInvalidaException("Debe iniciar sesión nuevamente.");
        }
        boolean esPropietario = solicitud.getUsuario() != null
            && solicitud.getUsuario().getUsuId().equals(usuarioAutenticado.getUsuId());
        if (!solicitud.estaPendiente() || !esPropietario) {
            throw new SolicitudInvalidaException("Solo puede editar sus solicitudes pendientes.");
        }
    }

    private SolicitudCambioEstadoDTO crearCambioEstado(String estadoNombre, Long responsableId, String observacion) {
        SolicitudCambioEstadoDTO dto = new SolicitudCambioEstadoDTO();
        dto.setNuevoEstadoId(obtenerEstadoIdPorNombre(estadoNombre));
        dto.setResponsableId(responsableId);
        dto.setObservacion(observacion);
        return dto;
    }

    private void registrarHistorial(Solicitudes solicitud, String estadoAnteriorNombre, SoliEstados estadoNuevo,
                                    Usuario usuario, String observacion) {
        SolicitudHistorialEstados historial = new SolicitudHistorialEstados();
        historial.setSolicitud(solicitud);
        historial.setEstadoAnterior(estadoAnteriorNombre != null ? estadosRepository.findBySoliEstadoNom(estadoAnteriorNombre).orElse(null) : null);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setUsuario(usuario);
        historial.setObservaciones(observacion);
        historialEstadosRepository.save(historial);
    }

    private String normalizarTexto(String texto) {
        return texto != null ? texto.trim() : null;
    }

    public boolean esSuperAdmin(Usuario usuario) {
        return usuario != null && usuario.getRol() != null
            && "Administrador".equalsIgnoreCase(usuario.getRol().getRolNom());
    }

    public boolean esTecnico(Usuario usuario) {
        return usuario != null && usuario.getRol() != null
            && "Tecnico Soporte".equalsIgnoreCase(usuario.getRol().getRolNom());
    }

    public boolean esStandardUser(Usuario usuario) {
        return usuario != null && usuario.getRol() != null
            && "Desarrollador".equalsIgnoreCase(usuario.getRol().getRolNom());
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
