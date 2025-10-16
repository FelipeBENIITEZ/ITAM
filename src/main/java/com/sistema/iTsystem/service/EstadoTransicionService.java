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
import com.sistema.iTsystem.model.ActivoHistorialEstados;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoHistorialEstadosRepository;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriaEstadoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;

@Service
public class EstadoTransicionService {

    // ==================== EXCEPCIONES PERSONALIZADAS ====================

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

    // ==================== AUTOWIRED ====================

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private ActivoHistorialEstadosRepository historialRepository;

    @Autowired
    private CategoriaEstadoRepository categoriaEstadoRepository;

    // ==================== FSM: TRANSICIONES PERMITIDAS ====================

    private static final Map<String, List<String>> TRANSICIONES_PERMITIDAS = new HashMap<>();

    static {
        // ==================== HARDWARE ====================
        
        TRANSICIONES_PERMITIDAS.put("Proyectado", List.of(
            "Activo"
        ));
        
        TRANSICIONES_PERMITIDAS.put("Activo", List.of(
            "En Mantenimiento",
            "Fuera de Servicio",
            "Pendiente de Baja"
        ));
        
        TRANSICIONES_PERMITIDAS.put("En Mantenimiento", List.of(
            "Activo",
            "Fuera de Servicio",
            "Pendiente de Baja"
        ));
        
        TRANSICIONES_PERMITIDAS.put("Fuera de Servicio", List.of(
            "Activo",
            "En Mantenimiento",
            "Pendiente de Baja",
            "Baja"
        ));
        
        TRANSICIONES_PERMITIDAS.put("Pendiente de Baja", List.of(
            "Activo",
            "Baja"
        ));
        
        // "Baja" es estado final (sin transiciones)
        TRANSICIONES_PERMITIDAS.put("Baja", List.of());
        
        // ==================== SERVICIOS ====================
        
        TRANSICIONES_PERMITIDAS.put("Suspendido", List.of(
            "Activo",
            "Cancelado"
        ));
        
        TRANSICIONES_PERMITIDAS.put("En Renovación", List.of(
            "Activo",
            "Vencido"
        ));
        
        TRANSICIONES_PERMITIDAS.put("Vencido", List.of(
            "Activo",
            "En Renovación",
            "Cancelado"
        ));
        
        // "Cancelado" es estado final (sin transiciones)
        TRANSICIONES_PERMITIDAS.put("Cancelado", List.of());
        
        // ==================== CONTRATOS ====================
        
        TRANSICIONES_PERMITIDAS.put("Vigente", List.of(
            "No Vigente",
            "Pendiente de Baja"
        ));
        
        TRANSICIONES_PERMITIDAS.put("No Vigente", List.of(
            "Vigente",
            "Pendiente de Baja",
            "Cancelado"
        ));
    }

    // ==================== CAMBIO DE ESTADO ====================

    /**
     * Cambiar estado de un activo
     */
    @Transactional
    public boolean cambiarEstado(Long activoId, Long nuevoEstadoId, String motivo, 
                                String observaciones, Usuario usuario) {
        
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new ActivoNoEncontradoException(
                "Activo con ID " + activoId + " no encontrado"
            ));
        
        EstadoActivo nuevoEstado = estadoActivoRepository.findById(nuevoEstadoId)
            .orElseThrow(() -> new TransicionInvalidaException(
                "Estado con ID " + nuevoEstadoId + " no encontrado"
            ));
        
        // Validar transición
        validarTransicion(activo, nuevoEstado);
        
        // Guardar en historial
        guardarHistorial(activo, nuevoEstado, motivo, observaciones, usuario);
        
        // Cambiar estado
        activo.setEstado(nuevoEstado);
        activoRepository.save(activo);
        
        return true;
    }

    // ==================== VALIDACIONES ====================

    /**
     * Validar que la transición sea permitida
     */
    private void validarTransicion(Activo activo, EstadoActivo nuevoEstado) {
        EstadoActivo estadoActual = activo.getEstado();
        
        //Validar que el nuevo estado sea válido para la categoría
        boolean esValidoParaCategoria = categoriaEstadoRepository
            .existsByCategoriaIdAndEstadoId(
                activo.getCategoria().getCatId(), 
                nuevoEstado.getEstadoId()
            );
        
        if (!esValidoParaCategoria) {
            throw new TransicionInvalidaException(
                "El estado '" + nuevoEstado.getEstadoNom() + 
                "' no es válido para la categoría '" + activo.getCategoria().getCatNom() + "'"
            );
        }
        
        // Primera asignación de estado
        if (estadoActual == null) {
            return;
        }
        
        String nombreEstadoActual = estadoActual.getEstadoNom();
        String nombreNuevoEstado = nuevoEstado.getEstadoNom();
        
        // Validar que "Baja" y "Cancelado" sean irrevocables
        if ("Baja".equals(nombreEstadoActual) || "Cancelado".equals(nombreEstadoActual)) {
            throw new TransicionInvalidaException(
                "No se puede cambiar el estado desde '" + nombreEstadoActual + 
                "'. Este estado es IRREVERSIBLE."
            );
        }
        
        // Validar transición según FSM
        List<String> transicionesPermitidas = TRANSICIONES_PERMITIDAS.get(nombreEstadoActual);
        
        if (transicionesPermitidas == null || !transicionesPermitidas.contains(nombreNuevoEstado)) {
            throw new TransicionInvalidaException(
                "No se puede cambiar de '" + nombreEstadoActual + "' a '" + nombreNuevoEstado + "'"
            );
        }
    }

    // ==================== HISTORIAL ====================

    /**
     * Guardar registro en historial
     */
    private void guardarHistorial(Activo activo, EstadoActivo nuevoEstado, String motivo, 
                                  String observaciones, Usuario usuario) {
        ActivoHistorialEstados historial = new ActivoHistorialEstados();
        historial.setActivo(activo);
        historial.setEstadoAnterior(activo.getEstado());
        historial.setEstadoNuevo(nuevoEstado);
        historial.setMotivo(motivo);
        historial.setObservaciones(observaciones);
        historial.setUsuario(usuario);
        historial.setFechaCambio(LocalDateTime.now());
        
        historialRepository.save(historial);
    }

    /**
     * Obtener historial de estados de un activo
     */
    public List<ActivoHistorialEstados> obtenerHistorialEstados(Long activoId) {
        return historialRepository.findByActivoIdWithDetails(activoId);
    }

    // ==================== ESTADOS POSIBLES ====================

    /**
     * Obtener estados posibles para un activo según su categoría y estado actual
     */
    public List<EstadoActivo> obtenerEstadosPosibles(Long activoId) {
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new ActivoNoEncontradoException(
                "Activo con ID " + activoId + " no encontrado"
            ));
        
        EstadoActivo estadoActual = activo.getEstado();
        
        if (estadoActual == null) {
            // Si no tiene estado, devolver estados iniciales de su categoría
            return categoriaEstadoRepository.findEstadosByCategoriaId(
                activo.getCategoria().getCatId()
            );
        }
        
        // Obtener transiciones permitidas según FSM
        String nombreEstadoActual = estadoActual.getEstadoNom();
        List<String> nombresEstadosPosibles = TRANSICIONES_PERMITIDAS.get(nombreEstadoActual);
        
        if (nombresEstadosPosibles == null || nombresEstadosPosibles.isEmpty()) {
            return List.of();
        }
        
        // ✅ Filtrar por: (1) FSM y (2) Categoría
        List<EstadoActivo> estadosDeCategoria = categoriaEstadoRepository
            .findEstadosByCategoriaId(activo.getCategoria().getCatId());
        
        return estadosDeCategoria.stream()
            .filter(estado -> nombresEstadosPosibles.contains(estado.getEstadoNom()))
            .collect(Collectors.toList());
    }

    /**
     * Verificar si una transición es válida
     */
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