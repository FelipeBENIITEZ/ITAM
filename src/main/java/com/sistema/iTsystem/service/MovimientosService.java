package com.sistema.iTsystem.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;

@Service
public class MovimientosService {

    @Autowired
    private ActivoService activoService;

    @Autowired
    private EstadoTransicionService estadoTransicionService;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private ActivoRepository activoRepository;

    public List<UsuarioAsignacion> obtenerAsignacionesActivas() {
        return usuarioAsignacionRepository.findAsignacionesActivasConDetalles();
    }

    public List<Activo> obtenerActivosDisponiblesParaAsignar() {
        Set<Long> activosAsignados = usuarioAsignacionRepository.findAsignacionesActivasConDetalles().stream()
            .map(UsuarioAsignacion::getActivo)
            .filter(activo -> activo != null && activo.getActivoId() != null)
            .map(Activo::getActivoId)
            .collect(Collectors.toCollection(HashSet::new));

        return activoService.obtenerTodos().stream()
            .filter(activo -> activo.getEstado() != null)
            .filter(activo -> "Disponible".equalsIgnoreCase(activo.getEstado().getEstadoNom()))
            .filter(activo -> Boolean.TRUE.equals(activo.getActivoActivo()))
            .filter(activo -> !activosAsignados.contains(activo.getActivoId()))
            .toList();
    }

    public List<Usuario> obtenerUsuariosActivos() {
        return usuarioRepository.findByUsuActivoTrueOrderByUsuLoginAsc();
    }

    public Optional<UsuarioAsignacion> obtenerAsignacionActiva(Long activoId) {
        return usuarioAsignacionRepository.findByActivoIdWithUserDetails(activoId).stream()
            .filter(asignacion -> Boolean.TRUE.equals(asignacion.getAsignacionActiva()))
            .findFirst();
    }

    public boolean tieneAsignacionActiva(Long activoId) {
        return usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(activoId).isPresent();
    }

    @Transactional
    public UsuarioAsignacion asignarActivo(Long activoId, Long usuarioId, LocalDate fechaAsignacion,
                                           String motivo, String observacion, Usuario usuarioOperador) {
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

        Usuario usuarioAsignado = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (fechaAsignacion == null) {
            throw new RuntimeException("Debe ingresar una fecha de asignación.");
        }

        if (activo.getActivoFechaIngreso() != null
                && fechaAsignacion.isBefore(activo.getActivoFechaIngreso().toLocalDate())) {
            throw new RuntimeException("La fecha de asignación no puede ser anterior a la fecha de ingreso del activo.");
        }

        if (motivo == null || motivo.trim().isEmpty()) {
            throw new RuntimeException("Debe ingresar un motivo.");
        }

        if (usuarioAsignado.getUsuActivo() == null || !Boolean.TRUE.equals(usuarioAsignado.getUsuActivo())) {
            throw new RuntimeException("El usuario seleccionado no está activo.");
        }

        obtenerEstado("Disponible");
        EstadoActivo estadoAsignado = obtenerEstado("Asignado");

        if (activo.getEstado() == null || !"Disponible".equalsIgnoreCase(activo.getEstado().getEstadoNom())) {
            throw new RuntimeException("El activo no está disponible para asignación.");
        }

        if (tieneAsignacionActiva(activoId)) {
            throw new RuntimeException("El activo ya tiene una asignación activa.");
        }

        estadoTransicionService.cambiarEstado(activoId, estadoAsignado.getEstadoId(), motivo, observacion, usuarioOperador);

        UsuarioAsignacion asignacion = new UsuarioAsignacion();
        asignacion.setActivo(activo);
        asignacion.setUsuario(usuarioAsignado);
        asignacion.setAsignacionFecha(fechaAsignacion);
        asignacion.setAsignacionMotivo(motivo);
        asignacion.setAsignacionObservacion(observacion);
        asignacion.setAsignacionActiva(true);

        return usuarioAsignacionRepository.save(asignacion);
    }

    @Transactional
    public UsuarioAsignacion devolverActivo(Long asignacionId, String motivo,
                                            String observacion, Usuario usuarioOperador) {
        UsuarioAsignacion asignacion = usuarioAsignacionRepository.findById(asignacionId)
            .orElseThrow(() -> new RuntimeException("Asignacion no encontrada"));

        if (!Boolean.TRUE.equals(asignacion.getAsignacionActiva())) {
            throw new RuntimeException("La asignacion ya fue finalizada");
        }

        EstadoActivo estadoDisponible = obtenerEstado("Disponible");
        estadoTransicionService.cambiarEstado(
            asignacion.getActivo().getActivoId(),
            estadoDisponible.getEstadoId(),
            motivo,
            observacion,
            usuarioOperador
        );

        asignacion.setAsignacionActiva(false);
        asignacion.setDevolucionFecha(LocalDate.now());
        if (observacion != null && !observacion.trim().isEmpty()) {
            asignacion.setAsignacionObservacion(observacion);
        }
        if (motivo != null && !motivo.trim().isEmpty()) {
            asignacion.setAsignacionMotivo(motivo);
        }

        return usuarioAsignacionRepository.save(asignacion);
    }

    @Transactional
    public void registrarCreacion(Long activoId, Usuario usuario, String motivo, String observacion) {
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
        estadoTransicionService.registrarCreacion(activo, usuario, motivo, observacion);
    }

    private EstadoActivo obtenerEstado(String nombre) {
        return estadoActivoRepository.findByEstadoNom(nombre)
            .orElseThrow(() -> new RuntimeException("No se encontró el estado " + nombre + "."));
    }
}
