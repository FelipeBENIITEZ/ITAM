package com.sistema.iTsystem.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoRepository;
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

        EstadoActivo nuevoEstado = estadoActivoRepository.findById(nuevoEstadoId)
            .orElseThrow(() -> new TransicionInvalidaException("Estado con ID " + nuevoEstadoId + " no encontrado"));

        validarTransicion(activo, nuevoEstado);
        activo.setEstado(nuevoEstado);
        activoRepository.save(activo);

        return true;
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

    public List<Object> obtenerHistorialEstados(Long activoId) {
        return List.of();
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
}
