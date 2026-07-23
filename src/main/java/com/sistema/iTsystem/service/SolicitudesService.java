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
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.SoliEstados;
import com.sistema.iTsystem.model.SoliTipos;
import com.sistema.iTsystem.model.SolicitudHistorialEstados;
import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.model.Marca;
import com.sistema.iTsystem.model.Modelo;
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.MarcaRepository;
import com.sistema.iTsystem.repository.ModeloRepository;
import com.sistema.iTsystem.repository.SolicitudHistorialEstadosRepository;
import com.sistema.iTsystem.repository.SoliEstadosRepository;
import com.sistema.iTsystem.repository.SoliTiposRepository;
import com.sistema.iTsystem.repository.SolicitudesRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.EstadoTransicionService;
import com.sistema.iTsystem.service.MovimientosService;

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
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ModeloRepository modeloRepository;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    @Autowired
    private HardwareInfoRepository hardwareInfoRepository;

    @Autowired
    private EstadoTransicionService estadoTransicionService;

    @Autowired
    private MovimientosService movimientosService;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
        "Mantenimiento",
        "Baja",
        "Compra",
        "Asignación",
        "Reasignación"
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
    private static final Set<String> TIPOS_FLUJO_PERMITIDOS = Set.of(
        "Mantenimiento",
        "Baja",
        "Compra",
        "Asignación",
        "Reasignación"
    );

    private static final Set<String> ESTADOS_FLUJO_PERMITIDOS = Set.of(
        "Pendiente",
        "En análisis",
        "Aprobada",
        "En ejecución",
        "Resuelta",
        "Rechazada",
        "Cerrada"
    );

    private static final Map<String, Set<String>> TRANSICIONES_FLUJO_PERMITIDAS = new HashMap<>();

    static {
        TRANSICIONES_FLUJO_PERMITIDAS.put("Pendiente", Set.of("En análisis", "Rechazada"));
        TRANSICIONES_FLUJO_PERMITIDAS.put("En análisis", Set.of("Aprobada", "Rechazada"));
        TRANSICIONES_FLUJO_PERMITIDAS.put("Aprobada", Set.of("En ejecución", "Resuelta"));
        TRANSICIONES_FLUJO_PERMITIDAS.put("En ejecución", Set.of("Resuelta"));
        TRANSICIONES_FLUJO_PERMITIDAS.put("Resuelta", Set.of("Cerrada"));
        TRANSICIONES_FLUJO_PERMITIDAS.put("Rechazada", Set.of());
        TRANSICIONES_FLUJO_PERMITIDAS.put("Cerrada", Set.of());
    }

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

    public List<Activo> obtenerActivosDisponiblesParaSolicitud() {
        Set<Long> activosAsignados = usuarioAsignacionRepository.findAsignacionesActivasConDetalles().stream()
            .map(UsuarioAsignacion::getActivo)
            .filter(activo -> activo != null && activo.getActivoId() != null)
            .map(Activo::getActivoId)
            .collect(Collectors.toSet());

        return activoRepository.findAll().stream()
            .filter(activo -> Boolean.TRUE.equals(activo.getActivoActivo()))
            .filter(activo -> activo.getEstado() != null)
            .filter(activo -> "Disponible".equalsIgnoreCase(activo.getEstado().getEstadoNom()))
            .filter(activo -> !activosAsignados.contains(activo.getActivoId()))
            .sorted(Comparator.comparing(activo -> activo.getActivoCodigo() != null ? activo.getActivoCodigo().toLowerCase() : ""))
            .toList();
    }

    public List<Activo> obtenerActivosParaReferencia() {
        return activoRepository.findAll().stream()
            .filter(activo -> Boolean.TRUE.equals(activo.getActivoActivo()))
            .filter(activo -> activo.getEstado() != null)
            .filter(activo -> !"Dado de baja".equalsIgnoreCase(activo.getEstado().getEstadoNom()))
            .sorted(Comparator.comparing(activo -> activo.getActivoCodigo() != null ? activo.getActivoCodigo().toLowerCase() : ""))
            .toList();
    }

    public List<Activo> obtenerActivosAsignadosConAsignacionActiva() {
        Set<Long> vistos = new HashSet<>();
        return usuarioAsignacionRepository.findAsignacionesActivasConDetalles().stream()
            .map(UsuarioAsignacion::getActivo)
            .filter(activo -> activo != null && activo.getActivoId() != null)
            .filter(activo -> vistos.add(activo.getActivoId()))
            .sorted(Comparator.comparing(activo -> activo.getActivoCodigo() != null ? activo.getActivoCodigo().toLowerCase() : ""))
            .toList();
    }

    public List<Usuario> obtenerUsuariosActivos() {
        return usuarioRepository.findAll().stream()
            .filter(usuario -> Boolean.TRUE.equals(usuario.getUsuActivo()))
            .sorted(Comparator.comparing(usuario -> usuario.getUsuLogin() != null ? usuario.getUsuLogin().toLowerCase() : ""))
            .toList();
    }

    public List<Usuario> obtenerUsuariosDestinatariosDisponibles() {
        return obtenerUsuariosActivos();
    }

    public Optional<UsuarioAsignacion> obtenerAsignacionActivaDeActivo(Long activoId) {
        if (activoId == null) {
            return Optional.empty();
        }
        return usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(activoId);
    }

    public Usuario obtenerUsuarioActualAsignado(Long activoId) {
        return obtenerAsignacionActivaDeActivo(activoId)
            .map(UsuarioAsignacion::getUsuario)
            .orElse(null);
    }

    public List<Usuario> obtenerResponsablesDisponibles() {
        return obtenerUsuariosActivos();
    }

    public List<Activo> obtenerActivosReferenciales() {
        return activoRepository.findAll().stream()
            .filter(activo -> Boolean.TRUE.equals(activo.getActivoActivo()))
            .sorted(Comparator.comparing(activo -> activo.getActivoCodigo() != null ? activo.getActivoCodigo().toLowerCase() : ""))
            .toList();
    }

    public List<Activo> obtenerActivosReferencialesConDatosTecnicos() {
        return obtenerActivosReferenciales().stream()
            .filter(activo -> activo != null && activo.getActivoId() != null)
            .filter(activo -> hardwareInfoRepository.findByActivo_ActivoId(activo.getActivoId()).isPresent())
            .toList();
    }

    private void aplicarCamposEspecificosPorTipoV2(Solicitudes solicitud, SolicitudFormDTO form, SoliTipos tipo, Usuario usuarioAutenticado) {
        String nombreTipo = tipo.getSoliTipoNom();

        if ("Mantenimiento".equalsIgnoreCase(nombreTipo) || "Baja".equalsIgnoreCase(nombreTipo)) {
            Activo activo = validarActivoObligatorioParaSolicitud(form.getActivoId());
            if ("Mantenimiento".equalsIgnoreCase(nombreTipo)) {
                String estadoActivo = activo.getEstado() != null ? activo.getEstado().getEstadoNom() : null;
                if (estadoActivo == null || !("Disponible".equalsIgnoreCase(estadoActivo) || "Asignado".equalsIgnoreCase(estadoActivo))) {
                    throw new SolicitudInvalidaException("El activo debe estar disponible o asignado para solicitar mantenimiento.");
                }
            }
            solicitud.setActivo(activo);
            solicitud.setMarca(null);
            solicitud.setModelo(null);
            solicitud.setSoliCantidad(null);
            solicitud.setUsuarioDestino(null);
            return;
        }

        if ("Compra".equalsIgnoreCase(nombreTipo)) {
            validarCantidad(form.getSoliCantidad(), true);
            solicitud.setSoliCantidad(form.getSoliCantidad());
            solicitud.setUsuarioDestino(null);

            if (form.getActivoId() != null) {
                Activo referencia = validarActivoOpcional(form.getActivoId());
                HardwareInfo hardwareReferencia = obtenerHardwareDeActivo(referencia);
                if (hardwareReferencia == null || hardwareReferencia.getModelo() == null || hardwareReferencia.getModelo().getMarca() == null) {
                    throw new SolicitudInvalidaException("El activo de referencia no tiene datos técnicos registrados.");
                }
                solicitud.setActivo(referencia);
                solicitud.setMarca(hardwareReferencia.getModelo().getMarca());
                solicitud.setModelo(hardwareReferencia.getModelo());
            } else {
                validarMarcaYModelo(form.getMarcaId(), form.getModelId());
                solicitud.setActivo(null);
                solicitud.setMarca(validarMarcaOpcional(form.getMarcaId()));
                solicitud.setModelo(validarModeloOpcional(form.getModelId(), solicitud.getMarca()));
            }
            return;
        }

        if ("Asignación".equalsIgnoreCase(nombreTipo)) {
            if (form.getSoliCantidad() != null && form.getSoliCantidad() != 1) {
                throw new SolicitudInvalidaException("La cantidad para una solicitud de asignación debe ser 1.");
            }
            Activo activo = validarActivoObligatorioDisponible(form.getActivoId());
            solicitud.setActivo(activo);
            solicitud.setSoliCantidad(1);
            solicitud.setMarca(null);
            solicitud.setModelo(null);
            solicitud.setUsuarioDestino(validarUsuarioActivo(form.getUsuarioDestinoId(), "El usuario destinatario no existe."));
            return;
        }

        if ("Reasignación".equalsIgnoreCase(nombreTipo)) {
            Activo activo = validarActivoObligatorioAsignado(form.getActivoId());
            UsuarioAsignacion asignacionActiva = usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(activo.getActivoId())
                .orElseThrow(() -> new SolicitudInvalidaException("El activo seleccionado no tiene una asignación activa."));
            Usuario usuarioActual = asignacionActiva.getUsuario();
            if (form.getUsuarioDestinoId() == null) {
                throw new SolicitudInvalidaException("Debe seleccionar el nuevo usuario destinatario.");
            }
            if (usuarioActual != null && usuarioActual.getUsuId() != null && usuarioActual.getUsuId().equals(form.getUsuarioDestinoId())) {
                throw new SolicitudInvalidaException("El nuevo usuario destinatario debe ser distinto al usuario actual.");
            }
            solicitud.setActivo(activo);
            solicitud.setUsuarioDestino(validarUsuarioActivo(form.getUsuarioDestinoId(), "El usuario destinatario no existe."));
            solicitud.setSoliCantidad(1);
            solicitud.setMarca(null);
            solicitud.setModelo(null);
            return;
        }

        throw new SolicitudInvalidaException("El tipo seleccionado no está permitido.");
    }

    private Activo validarActivoObligatorioParaSolicitud(Long activoId) {
        if (activoId == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un activo.");
        }
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new SolicitudInvalidaException("El activo seleccionado no existe."));
        if (!Boolean.TRUE.equals(activo.getActivoActivo())) {
            throw new SolicitudInvalidaException("El activo seleccionado ya está dado de baja.");
        }
        if (activo.getEstado() != null && "Dado de baja".equalsIgnoreCase(activo.getEstado().getEstadoNom())) {
            throw new SolicitudInvalidaException("El activo seleccionado ya está dado de baja.");
        }
        return activo;
    }

    private HardwareInfo obtenerHardwareDeActivo(Activo activo) {
        if (activo == null || activo.getActivoId() == null) {
            return null;
        }
        return hardwareInfoRepository.findByActivo_ActivoId(activo.getActivoId()).orElse(null);
    }

    public List<SoliTipos> obtenerTiposFlujo() {
        return tiposRepository.findAll().stream()
            .filter(tipo -> tipo.getSoliTipoNom() != null && TIPOS_FLUJO_PERMITIDOS.contains(tipo.getSoliTipoNom()))
            .sorted(Comparator.comparing(SoliTipos::getSoliTipoNom))
            .toList();
    }

    public List<SoliEstados> obtenerEstadosFlujo() {
        return estadosRepository.findAll().stream()
            .filter(estado -> estado.getSoliEstadoNom() != null && ESTADOS_FLUJO_PERMITIDOS.contains(estado.getSoliEstadoNom()))
            .sorted(Comparator.comparing(SoliEstados::getSoliEstadoNom))
            .toList();
    }

    public Solicitudes asignarResponsableYEnviarAAnalisis(Long solicitudId, Long responsableId,
                                                          Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }

        Usuario responsable = validarUsuarioActivo(responsableId, "El responsable seleccionado no existe.");
        String estadoActual = solicitud.getEstadoSolicitud();

        if (solicitud.estaPendiente()) {
            String nuevoEstado = "En análisis";
            validarTransicionFlujo(estadoActual, nuevoEstado);
            solicitud.setResponsable(responsable);
            return transicionarSolicitud(solicitud, nuevoEstado, usuarioAutenticado,
                observacion != null && !observacion.trim().isEmpty()
                    ? observacion.trim()
                    : "Asignación de responsable y envío a análisis");
        }

        if (solicitud.estaEnAnalisis()) {
            solicitud.setResponsable(responsable);
            solicitudesRepository.save(solicitud);
            registrarHistorial(solicitud, estadoActual, obtenerEstadoObligatorio("En análisis"), usuarioAutenticado,
                observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Cambio de responsable");
            return solicitud;
        }

        throw new SolicitudInvalidaException("Solo se puede asignar responsable en solicitudes pendientes o en análisis.");
    }

    public Solicitudes aprobarSolicitud(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }
        if (!solicitud.estaEnAnalisis()) {
            throw new SolicitudInvalidaException("Solo se pueden aprobar solicitudes en análisis.");
        }
        return transicionarSolicitud(solicitud, "Aprobada", usuarioAutenticado,
            observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Solicitud aprobada");
    }

    public Solicitudes rechazarSolicitud(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }
        if (!solicitud.estaPendiente() && !solicitud.estaEnAnalisis()) {
            throw new SolicitudInvalidaException("Solo se pueden rechazar solicitudes pendientes o en análisis.");
        }
        if (observacion == null || observacion.trim().isEmpty()) {
            throw new SolicitudInvalidaException("La observación de rechazo es obligatoria.");
        }
        return transicionarSolicitud(solicitud, "Rechazada", usuarioAutenticado, observacion.trim());
    }

    public Solicitudes iniciarOperacion(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }
        if (!solicitud.estaAprobada()) {
            throw new SolicitudInvalidaException("Solo se pueden iniciar operaciones desde solicitudes aprobadas.");
        }

        String tipo = solicitud.getTipoSolicitud();
        if ("Mantenimiento".equalsIgnoreCase(tipo)) {
            ejecutarInicioMantenimiento(solicitud, usuarioAutenticado, observacion);
            return solicitud;
        }
        if ("Compra".equalsIgnoreCase(tipo)) {
            return transicionarSolicitud(solicitud, "En ejecución", usuarioAutenticado,
                observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Compra en ejecución");
        }

        throw new SolicitudInvalidaException("El tipo seleccionado no admite inicio de operación.");
    }

    public Solicitudes resolverOperacion(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }

        String tipo = solicitud.getTipoSolicitud();
        if ("Mantenimiento".equalsIgnoreCase(tipo)) {
            ejecutarFinMantenimiento(solicitud, usuarioAutenticado, observacion);
            return solicitud;
        }
        if ("Compra".equalsIgnoreCase(tipo)) {
            if (!solicitud.estaEnEjecucion()) {
                throw new SolicitudInvalidaException("La compra debe estar en ejecución para resolverse.");
            }
            if (observacion == null || observacion.trim().isEmpty()) {
                throw new SolicitudInvalidaException("La observación de resolución es obligatoria.");
            }
            return transicionarSolicitud(solicitud, "Resuelta", usuarioAutenticado, observacion.trim());
        }
        if ("Asignación".equalsIgnoreCase(tipo)) {
            ejecutarAsignacionSolicitud(solicitud.getSoliId(), usuarioAutenticado, observacion);
            return solicitud;
        }
        if ("Reasignación".equalsIgnoreCase(tipo)) {
            ejecutarReasignacionSolicitud(solicitud.getSoliId(), usuarioAutenticado, observacion);
            return solicitud;
        }

        throw new SolicitudInvalidaException("El tipo seleccionado no admite resolución por esta vía.");
    }

    public Solicitudes ejecutarBajaSolicitud(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }
        if (!"Baja".equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new SolicitudInvalidaException("La solicitud no corresponde a una baja.");
        }
        if (!solicitud.estaAprobada()) {
            throw new SolicitudInvalidaException("La baja solo puede ejecutarse desde una solicitud aprobada.");
        }
        if (solicitud.getActivo() == null) {
            throw new SolicitudInvalidaException("La solicitud no tiene un activo asociado.");
        }
        Optional<UsuarioAsignacion> asignacionActiva = usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(solicitud.getActivo().getActivoId());
        asignacionActiva.ifPresent(asignacion -> movimientosService.cerrarAsignacionActiva(
            solicitud.getActivo().getActivoId(),
            solicitud.getSoliMotivo(),
            observacion,
            usuarioAutenticado,
            solicitud
        ));

        estadoTransicionService.cambiarEstado(
            solicitud.getActivo().getActivoId(),
            obtenerEstadoActivoId("Dado de baja"),
            solicitud.getSoliMotivo(),
            observacion,
            usuarioAutenticado
        );

        return transicionarSolicitud(solicitud, "Resuelta", usuarioAutenticado,
            observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Baja ejecutada");
    }

    public Solicitudes ejecutarAsignacionSolicitud(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }
        if (!"Asignación".equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new SolicitudInvalidaException("La solicitud no corresponde a una asignación.");
        }
        if (!solicitud.estaAprobada()) {
            throw new SolicitudInvalidaException("La asignación solo puede ejecutarse desde una solicitud aprobada.");
        }
        if (solicitud.getActivo() == null || solicitud.getUsuarioDestino() == null) {
            throw new SolicitudInvalidaException("La solicitud debe tener activo y destinatario definidos.");
        }

        try {
            movimientosService.asignarActivo(
                solicitud.getActivo().getActivoId(),
                solicitud.getUsuarioDestino().getUsuId(),
                solicitud.getSoliMotivo(),
                observacion,
                usuarioAutenticado,
                solicitud
            );
        } catch (RuntimeException e) {
            throw new SolicitudInvalidaException(e.getMessage());
        }

        return transicionarSolicitud(solicitud, "Resuelta", usuarioAutenticado,
            observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Asignación ejecutada");
    }

    public Solicitudes ejecutarReasignacionSolicitud(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }
        if (!"Reasignación".equalsIgnoreCase(solicitud.getTipoSolicitud())) {
            throw new SolicitudInvalidaException("La solicitud no corresponde a una reasignación.");
        }
        if (!solicitud.estaAprobada()) {
            throw new SolicitudInvalidaException("La reasignación solo puede ejecutarse desde una solicitud aprobada.");
        }
        if (solicitud.getActivo() == null || solicitud.getUsuarioDestino() == null) {
            throw new SolicitudInvalidaException("La solicitud debe tener activo y nuevo destinatario definidos.");
        }

        try {
            movimientosService.reasignarActivo(
                solicitud.getActivo().getActivoId(),
                solicitud.getUsuarioDestino().getUsuId(),
                LocalDate.now(),
                solicitud.getSoliMotivo(),
                observacion,
                usuarioAutenticado,
                solicitud
            );
        } catch (RuntimeException e) {
            throw new SolicitudInvalidaException(e.getMessage());
        }

        return transicionarSolicitud(solicitud, "Resuelta", usuarioAutenticado,
            observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Reasignación ejecutada");
    }

    public Solicitudes cerrarSolicitud(Long solicitudId, Usuario usuarioAutenticado, String observacion) {
        Solicitudes solicitud = solicitudesRepository.findByIdWithDetails(solicitudId);
        if (solicitud == null) {
            throw new SolicitudNoEncontradaException("Solicitud con ID " + solicitudId + " no encontrada");
        }
        if (!solicitud.estaResuelta()) {
            throw new SolicitudInvalidaException("Solo se pueden cerrar solicitudes resueltas.");
        }
        return transicionarSolicitud(solicitud, "Cerrada", usuarioAutenticado,
            observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Cierre administrativo");
    }

    private void ejecutarInicioMantenimiento(Solicitudes solicitud, Usuario usuarioAutenticado, String observacion) {
        if (solicitud.getActivo() == null) {
            throw new SolicitudInvalidaException("La solicitud no tiene un activo asociado.");
        }
        if (solicitud.getActivo().getEstado() == null) {
            throw new SolicitudInvalidaException("El activo no tiene un estado definido.");
        }

        usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(solicitud.getActivo().getActivoId())
            .ifPresent(asignacion -> movimientosService.cerrarAsignacionActiva(
                solicitud.getActivo().getActivoId(),
                solicitud.getSoliMotivo(),
                observacion,
                usuarioAutenticado,
                solicitud
            ));

        estadoTransicionService.cambiarEstado(
            solicitud.getActivo().getActivoId(),
            obtenerEstadoActivoId("En mantenimiento"),
            solicitud.getSoliMotivo(),
            observacion,
            usuarioAutenticado
        );

        transicionarSolicitud(solicitud, "En ejecución", usuarioAutenticado,
            observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Mantenimiento iniciado");
    }

    private void ejecutarFinMantenimiento(Solicitudes solicitud, Usuario usuarioAutenticado, String observacion) {
        if (!solicitud.estaEnEjecucion()) {
            throw new SolicitudInvalidaException("El mantenimiento debe estar en ejecución para finalizarse.");
        }
        estadoTransicionService.cambiarEstado(
            solicitud.getActivo().getActivoId(),
            obtenerEstadoActivoId("Disponible"),
            solicitud.getSoliMotivo(),
            observacion,
            usuarioAutenticado
        );
        transicionarSolicitud(solicitud, "Resuelta", usuarioAutenticado,
            observacion != null && !observacion.trim().isEmpty() ? observacion.trim() : "Mantenimiento finalizado");
    }

    private Solicitudes transicionarSolicitud(Solicitudes solicitud, String nuevoEstadoNombre,
                                              Usuario usuarioAutenticado, String observacion) {
        String estadoActual = solicitud.getEstadoSolicitud();
        validarTransicionFlujo(estadoActual, nuevoEstadoNombre);
        SoliEstados nuevoEstado = obtenerEstadoObligatorio(nuevoEstadoNombre);
        solicitud.setSoliEstado(nuevoEstado);
        solicitudesRepository.save(solicitud);
        registrarHistorial(solicitud, estadoActual, nuevoEstado, usuarioAutenticado, observacion);
        return solicitud;
    }

    private void validarTransicionFlujo(String estadoActual, String estadoNuevo) {
        if (estadoNuevo == null || estadoNuevo.isBlank()) {
            throw new SolicitudInvalidaException("La transición de estado solicitada no está permitida.");
        }
        if (estadoActual == null) {
            if (!"Pendiente".equalsIgnoreCase(estadoNuevo)) {
                throw new SolicitudInvalidaException("La transición de estado solicitada no está permitida.");
            }
            return;
        }
        Set<String> permitidos = TRANSICIONES_FLUJO_PERMITIDAS.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(estadoNuevo)) {
            throw new SolicitudInvalidaException("La transición de estado solicitada no está permitida.");
        }
    }

    private Long obtenerEstadoActivoId(String nombreEstado) {
        return estadoActivoRepository.findByEstadoNom(nombreEstado)
            .map(EstadoActivo::getEstadoId)
            .orElseThrow(() -> new SolicitudInvalidaException("Estado de activo '" + nombreEstado + "' no encontrado."));
    }

    private Usuario validarUsuarioActivo(Long usuarioId, String mensajeNoEncontrado) {
        if (usuarioId == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un usuario válido.");
        }
        return usuarioRepository.findById(usuarioId)
            .filter(usuario -> Boolean.TRUE.equals(usuario.getUsuActivo()))
            .orElseThrow(() -> new SolicitudInvalidaException(mensajeNoEncontrado));
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
        String textoNormalizado = texto != null ? texto.trim() : "";
        Long tipoFiltrado = tipoId != null ? tipoId : -1L;
        Long estadoFiltrado = estadoId != null ? estadoId : -1L;
        Long solicitanteFiltrado = solicitanteId != null ? solicitanteId : -1L;
        Long responsableFiltrado = responsableId != null ? responsableId : -1L;
        LocalDateTime desde = fechaDesde != null ? fechaDesde.atStartOfDay() : LocalDateTime.of(1900, 1, 1, 0, 0);
        LocalDateTime hasta = fechaHasta != null ? fechaHasta.atTime(LocalTime.MAX) : LocalDateTime.of(2999, 12, 31, 23, 59, 59);

        return solicitudesRepository.findWithFilters(
            textoNormalizado,
            tipoFiltrado,
            estadoFiltrado,
            solicitanteFiltrado,
            responsableFiltrado,
            desde,
            hasta,
            -1L,
            -1L,
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

        aplicarCamposEspecificosPorTipoV2(solicitud, formulario, tipo, usuarioAutenticado);
        solicitud = solicitudesRepository.save(solicitud);

        registrarHistorial(solicitud, null, solicitud.getSoliEstado(), usuarioAutenticado, "Creación de solicitud");
        return solicitud;
    }

    @Transactional
    public Solicitudes actualizarDesdeFormulario(Long id, SolicitudFormDTO form, Usuario usuarioAutenticado) {
        Solicitudes solicitud = solicitudesRepository.findById(id)
            .orElseThrow(() -> new SolicitudNoEncontradaException("Solicitud con ID " + id + " no encontrada"));

        validarEdicionPermitida(solicitud, usuarioAutenticado);
        throw new SolicitudInvalidaException("No se permite editar solicitudes enviadas.");
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

        if ("Rechazada".equalsIgnoreCase(nombreNuevoEstado)
                && (dto.getObservacion() == null || dto.getObservacion().trim().isEmpty())) {
            throw new SolicitudInvalidaException("La observación de rechazo es obligatoria.");
        }

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

    private void aplicarCamposEspecificosPorTipo(Solicitudes solicitud, SolicitudFormDTO form, SoliTipos tipo, Usuario usuarioAutenticado) {
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
            solicitud.setUsuarioDestino(null);
            return;
        }

        if ("Compra".equalsIgnoreCase(nombreTipo)) {
            validarCantidad(form.getSoliCantidad(), true);
            validarMarcaYModelo(form.getMarcaId(), form.getModelId());
            solicitud.setActivo(validarActivoOpcional(form.getActivoId()));
            solicitud.setSoliCantidad(form.getSoliCantidad());
            solicitud.setMarca(validarMarcaOpcional(form.getMarcaId()));
            solicitud.setModelo(validarModeloOpcional(form.getModelId(), solicitud.getMarca()));
            solicitud.setUsuarioDestino(null);
            return;
        }

        if ("Asignación".equalsIgnoreCase(nombreTipo)) {
            if (form.getSoliCantidad() != null && form.getSoliCantidad() != 1) {
                throw new SolicitudInvalidaException("La cantidad para una solicitud de asignación debe ser 1.");
            }
            Activo activo = validarActivoObligatorioDisponible(form.getActivoId());
            validarMarcaYModelo(form.getMarcaId(), form.getModelId());
            solicitud.setActivo(activo);
            solicitud.setSoliCantidad(1);
            solicitud.setMarca(validarMarcaOpcional(form.getMarcaId()));
            solicitud.setModelo(validarModeloOpcional(form.getModelId(), solicitud.getMarca()));
            solicitud.setUsuarioDestino(validarUsuarioDestinoAsignacion(form.getUsuarioDestinoId(), usuarioAutenticado));
            return;
        }

        if ("Reasignación".equalsIgnoreCase(nombreTipo)) {
            if (usuarioAutenticado != null && esStandardUser(usuarioAutenticado)) {
                throw new SolicitudInvalidaException("No tiene permisos para crear solicitudes de reasignación.");
            }
            Activo activo = validarActivoObligatorioAsignado(form.getActivoId());
            UsuarioAsignacion asignacionActiva = usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(activo.getActivoId())
                .orElseThrow(() -> new SolicitudInvalidaException("El activo seleccionado no tiene una asignación activa."));
            Usuario usuarioActual = asignacionActiva.getUsuario();
            Long usuarioDestinoId = form.getUsuarioDestinoId();
            if (usuarioDestinoId == null) {
                throw new SolicitudInvalidaException("Debe seleccionar el nuevo usuario destinatario.");
            }
            if (usuarioActual != null && usuarioActual.getUsuId() != null && usuarioActual.getUsuId().equals(usuarioDestinoId)) {
                throw new SolicitudInvalidaException("El nuevo usuario destinatario debe ser distinto al usuario actual.");
            }
            solicitud.setActivo(activo);
            solicitud.setUsuarioDestino(validarUsuarioDestinoAsignacion(usuarioDestinoId, usuarioAutenticado));
            solicitud.setSoliCantidad(1);
            solicitud.setMarca(null);
            solicitud.setModelo(null);
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

    private void validarMarcaYModelo(Long marcaId, Long modeloId) {
        if (modeloId != null && marcaId == null) {
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

    private Activo validarActivoObligatorioDisponible(Long activoId) {
        if (activoId == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un activo.");
        }
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new SolicitudInvalidaException("El activo seleccionado no existe."));
        if (!Boolean.TRUE.equals(activo.getActivoActivo())) {
            throw new SolicitudInvalidaException("El activo seleccionado no está disponible.");
        }
        if (activo.getEstado() == null || !"Disponible".equalsIgnoreCase(activo.getEstado().getEstadoNom())) {
            throw new SolicitudInvalidaException("El activo seleccionado no está disponible.");
        }
        if (usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(activoId).isPresent()) {
            throw new SolicitudInvalidaException("El activo ya tiene una asignación activa.");
        }
        return activo;
    }

    private Activo validarActivoObligatorioAsignado(Long activoId) {
        if (activoId == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un activo.");
        }
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new SolicitudInvalidaException("El activo seleccionado no existe."));
        if (activo.getEstado() == null || !"Asignado".equalsIgnoreCase(activo.getEstado().getEstadoNom())) {
            throw new SolicitudInvalidaException("El activo seleccionado no está asignado.");
        }
        if (usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(activoId).isEmpty()) {
            throw new SolicitudInvalidaException("El activo seleccionado no tiene una asignación activa.");
        }
        return activo;
    }

    private Usuario validarUsuarioDestinoAsignacion(Long usuarioDestinoId, Usuario usuarioAutenticado) {
        if (usuarioAutenticado != null && esStandardUser(usuarioAutenticado)) {
            if (usuarioDestinoId != null && !usuarioAutenticado.getUsuId().equals(usuarioDestinoId)) {
                throw new SolicitudInvalidaException("El usuario autenticado no puede seleccionar otro destinatario.");
            }
            return usuarioAutenticado;
        }
        if (usuarioDestinoId == null) {
            throw new SolicitudInvalidaException("Debe seleccionar un usuario destinatario.");
        }
        Usuario destinatario = usuarioRepository.findById(usuarioDestinoId)
            .orElseThrow(() -> new SolicitudInvalidaException("El usuario destinatario no existe."));
        if (!Boolean.TRUE.equals(destinatario.getUsuActivo())) {
            throw new SolicitudInvalidaException("El usuario destinatario debe estar activo.");
        }
        return destinatario;
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
            if (esSuperAdmin(actor) || esTecnico(actor)) {
                return;
            }
            throw new SolicitudInvalidaException("No tiene permisos para pasar esta solicitud a En análisis.");
        }

        if ("Aprobada".equalsIgnoreCase(nuevoEstado)
                || "Rechazada".equalsIgnoreCase(nuevoEstado)
                || "Cerrada".equalsIgnoreCase(nuevoEstado)) {
            if (!(esSuperAdmin(actor) || esTecnico(actor))) {
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
            && ("Tecnico Soporte".equalsIgnoreCase(usuario.getRol().getRolNom())
                || "Agente TI".equalsIgnoreCase(usuario.getRol().getRolNom())
                || "Tecnico".equalsIgnoreCase(usuario.getRol().getRolNom()));
    }

    public boolean esStandardUser(Usuario usuario) {
        return usuario != null && usuario.getRol() != null
            && "Desarrollador".equalsIgnoreCase(usuario.getRol().getRolNom());
    }

    public boolean esAgenteTi(Usuario usuario) {
        return esTecnico(usuario);
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
