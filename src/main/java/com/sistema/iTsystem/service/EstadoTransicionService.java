package com.sistema.iTsystem.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.ActivoHistorialEstados;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.ActivoHistorialEstadosRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;

@Service
public class EstadoTransicionService {

    public static class TransicionInvalidaException extends RuntimeException {
        public TransicionInvalidaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ActivoNoEncontradoException extends RuntimeException {
        public ActivoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private ActivoHistorialEstadosRepository historialEstadosRepository;

    private static final Map<String, List<String>> TRANSICIONES_PERMITIDAS = new HashMap<>();

    static {
        TRANSICIONES_PERMITIDAS.put("Disponible", List.of(
            "Asignado",
            "En mantenimiento",
            "Dado de baja",
            "Extraviado"
        ));
        TRANSICIONES_PERMITIDAS.put("Asignado", List.of(
            "Disponible",
            "En mantenimiento",
            "Dado de baja",
            "Extraviado"
        ));
        TRANSICIONES_PERMITIDAS.put("En mantenimiento", List.of(
            "Disponible",
            "Asignado",
            "Dado de baja"
        ));
        TRANSICIONES_PERMITIDAS.put("Extraviado", List.of(
            "Disponible",
            "Dado de baja"
        ));
        TRANSICIONES_PERMITIDAS.put("Dado de baja", List.of());
    }

    @Transactional
    public boolean cambiarEstado(Long activoId, Long nuevoEstadoId, String motivo,
                                 String observaciones, Usuario usuario) {
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new ActivoNoEncontradoException("Activo con ID " + activoId + " no encontrado"));

        EstadoActivo estadoAnterior = activo.getEstado();
        EstadoActivo nuevoEstado = estadoActivoRepository.findById(nuevoEstadoId)
            .orElseThrow(() -> new TransicionInvalidaException("Estado con ID " + nuevoEstadoId + " no encontrado"));

        validarTransicion(activo, nuevoEstado);
        activo.setEstado(nuevoEstado);
        if ("Dado de baja".equalsIgnoreCase(nuevoEstado.getEstadoNom())) {
            activo.setActivoActivo(false);
            activo.setActivoFechaEgreso(LocalDateTime.now());
        } else {
            activo.setActivoActivo(true);
        }
        activoRepository.save(activo);
        registrarHistorial(activo, estadoAnterior, nuevoEstado, usuario, motivo, observaciones);

        return true;
    }

    @Transactional
    public ActivoHistorialEstados enviarAMantenimiento(String activoCodigo, String motivo,
                                                       String observaciones, Usuario usuario) {
        return ejecutarOperacion(
            activoCodigo,
            "En mantenimiento",
            Set.of("Disponible"),
            motivo,
            observaciones,
            usuario,
            false
        );
    }

    @Transactional
    public ActivoHistorialEstados finalizarMantenimiento(String activoCodigo, String motivo,
                                                         String observaciones, Usuario usuario) {
        return ejecutarOperacion(
            activoCodigo,
            "Disponible",
            Set.of("En mantenimiento"),
            motivo,
            observaciones,
            usuario,
            false
        );
    }

    @Transactional
    public ActivoHistorialEstados darDeBaja(String activoCodigo, String motivo,
                                            String observaciones, Usuario usuario) {
        return ejecutarOperacion(
            activoCodigo,
            "Dado de baja",
            Set.of("Disponible", "En mantenimiento"),
            motivo,
            observaciones,
            usuario,
            true
        );
    }

    @Transactional
    public ActivoHistorialEstados registrarCreacion(Activo activo, Usuario usuario, String motivo, String observaciones) {
        if (activo == null) {
            throw new IllegalArgumentException("El activo es obligatorio");
        }
        if (activo.getEstado() == null) {
            throw new IllegalArgumentException("El activo debe tener un estado antes de registrar la creacion");
        }
        return registrarHistorial(activo, null, activo.getEstado(), usuario, motivo, observaciones);
    }

    private void validarTransicion(Activo activo, EstadoActivo nuevoEstado) {
        EstadoActivo estadoActual = activo.getEstado();
        if (estadoActual == null) {
            return;
        }

        String nombreEstadoActual = estadoActual.getEstadoNom();
        String nombreNuevoEstado = nuevoEstado.getEstadoNom();

        if ("Dado de baja".equalsIgnoreCase(nombreEstadoActual)) {
            throw new TransicionInvalidaException("No se puede cambiar el estado desde 'Dado de baja'");
        }

        List<String> transicionesPermitidas = TRANSICIONES_PERMITIDAS.get(nombreEstadoActual);
        if (transicionesPermitidas == null || !transicionesPermitidas.contains(nombreNuevoEstado)) {
            throw new TransicionInvalidaException(
                "No se puede cambiar de '" + nombreEstadoActual + "' a '" + nombreNuevoEstado + "'"
            );
        }
    }

    public List<ActivoHistorialEstados> obtenerHistorialEstados(Long activoId) {
        return historialEstadosRepository.findByActivoIdWithDetails(activoId);
    }

    public List<EstadoActivo> obtenerEstadosPosibles(Long activoId) {
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new ActivoNoEncontradoException("Activo con ID " + activoId + " no encontrado"));

        EstadoActivo estadoActual = activo.getEstado();
        if (estadoActual == null) {
            return estadoActivoRepository.findAllByOrderByEstadoNomAsc();
        }

        List<String> nombresEstadosPosibles = TRANSICIONES_PERMITIDAS.get(estadoActual.getEstadoNom());
        if (nombresEstadosPosibles == null || nombresEstadosPosibles.isEmpty()) {
            return List.of();
        }

        return estadoActivoRepository.findAllByOrderByEstadoNomAsc().stream()
            .filter(estado -> nombresEstadosPosibles.contains(estado.getEstadoNom()))
            .collect(Collectors.toList());
    }

    public boolean esTransicionValida(Long activoId, Long nuevoEstadoId) {
        try {
            Activo activo = activoRepository.findById(activoId)
                .orElseThrow(() -> new ActivoNoEncontradoException("Activo no encontrado"));

            EstadoActivo nuevoEstado = estadoActivoRepository.findById(nuevoEstadoId)
                .orElseThrow(() -> new TransicionInvalidaException("Estado no encontrado"));

            validarTransicion(activo, nuevoEstado);
            return true;
        } catch (TransicionInvalidaException e) {
            return false;
        }
    }

    private ActivoHistorialEstados ejecutarOperacion(String activoCodigo, String estadoDestinoNombre,
                                                     Set<String> estadosOrigenPermitidos, String motivo,
                                                     String observaciones, Usuario usuario, boolean esBaja) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw new TransicionInvalidaException("El motivo es obligatorio");
        }

        Activo activo = activoRepository.findByActivoCodigoIgnoreCase(normalizarCodigo(activoCodigo))
            .orElseThrow(() -> new ActivoNoEncontradoException("Activo con codigo " + activoCodigo + " no encontrado"));

        EstadoActivo estadoActual = activo.getEstado();
        if (estadoActual == null) {
            throw new TransicionInvalidaException("El activo no tiene un estado actual definido");
        }
        if (!estadosOrigenPermitidos.contains(estadoActual.getEstadoNom())) {
            throw new TransicionInvalidaException(
                "No se puede pasar de '" + estadoActual.getEstadoNom() + "' a '" + estadoDestinoNombre + "'"
            );
        }

        EstadoActivo nuevoEstado = estadoActivoRepository.findByEstadoNom(estadoDestinoNombre)
            .orElseThrow(() -> new TransicionInvalidaException("Estado '" + estadoDestinoNombre + "' no encontrado"));

        activo.setEstado(nuevoEstado);
        activo.setActivoActivo(!esBaja);
        if (esBaja) {
            activo.setActivoFechaEgreso(LocalDateTime.now());
        } else {
            activo.setActivoFechaEgreso(null);
        }
        activoRepository.save(activo);

        return registrarHistorial(activo, estadoActual, nuevoEstado, usuario, motivo, observaciones);
    }

    private ActivoHistorialEstados registrarHistorial(Activo activo, EstadoActivo estadoAnterior,
                                                      EstadoActivo estadoNuevo, Usuario usuario,
                                                      String motivo, String observaciones) {
        if (usuario == null) {
            throw new IllegalArgumentException("El usuario es obligatorio para registrar el historial");
        }

        ActivoHistorialEstados historial = new ActivoHistorialEstados();
        historial.setActivo(activo);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setUsuario(usuario);
        historial.setMotivo(motivo);
        historial.setObservaciones(observaciones);
        return historialEstadosRepository.save(historial);
    }

    private String normalizarCodigo(String activoCodigo) {
        if (activoCodigo == null) {
            return null;
        }

        return activoCodigo.trim();
    }
}
