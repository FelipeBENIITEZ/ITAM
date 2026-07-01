package com.sistema.iTsystem.service;

import java.time.LocalDate;
import java.util.List;

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
        return usuarioAsignacionRepository.findByAsignacionActivaTrueOrderByAsignacionFechaDesc();
    }

    public List<Activo> obtenerActivosDisponiblesParaAsignar() {
        return activoService.obtenerTodos().stream()
            .filter(activo -> activo.getEstado() != null)
            .filter(activo -> "Disponible".equalsIgnoreCase(activo.getEstado().getEstadoNom()))
            .filter(activo -> Boolean.TRUE.equals(activo.getActivoActivo()))
            .toList();
    }

    public List<Usuario> obtenerUsuariosActivos() {
        return usuarioRepository.findAll();
    }

    @Transactional
    public UsuarioAsignacion asignarActivo(Long activoId, Long usuarioId, String motivo,
                                           String observacion, Usuario usuarioOperador) {
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

        Usuario usuarioAsignado = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(activoId).isPresent()) {
            throw new RuntimeException("El activo ya tiene una asignacion activa");
        }

        EstadoActivo estadoAsignado = obtenerEstado("Asignado");
        estadoTransicionService.cambiarEstado(activoId, estadoAsignado.getEstadoId(), motivo, observacion, usuarioOperador);

        UsuarioAsignacion asignacion = new UsuarioAsignacion();
        asignacion.setActivo(activo);
        asignacion.setUsuario(usuarioAsignado);
        asignacion.setAsignacionFecha(LocalDate.now());
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
            .orElseThrow(() -> new RuntimeException("Estado '" + nombre + "' no encontrado"));
    }
}
