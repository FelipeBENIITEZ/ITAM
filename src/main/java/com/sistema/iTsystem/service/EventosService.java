package com.sistema.iTsystem.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.Eventos;
import com.sistema.iTsystem.model.EventosNiveles;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.EventosNivelesRepository;
import com.sistema.iTsystem.repository.EventosRepository;

@Service
public class EventosService {

    @Autowired
    private EventosRepository eventosRepository;
    
    @Autowired
    private EventosNivelesRepository nivelesRepository;
    
    @Autowired
    private ActivoRepository activoRepository;

    // ==================== CRUD BÁSICO ====================

    /**
     * Obtener todos los eventos
     */
    public List<Eventos> obtenerTodos() {
        return eventosRepository.findAll();
    }

    /**
     * Obtener todos los eventos con paginación
     */
    public Page<Eventos> obtenerTodos(Pageable pageable) {
        return eventosRepository.findAllByOrderByEventFechaDesc(pageable);
    }

    /**
     * Buscar evento por ID
     */
    public Optional<Eventos> buscarPorId(Long id) {
        return eventosRepository.findById(id);
    }

    /**
     * Buscar evento por ID con detalles (JOIN FETCH)
     */
    public Eventos buscarPorIdConDetalles(Long id) {
        return eventosRepository.findByIdWithDetails(id);
    }

    /**
     * Crear nuevo evento
     */
    @Transactional
    public Eventos crear(Eventos evento) {
        validarEvento(evento);
        
        // Si no tiene fecha, asignar hoy
        if (evento.getEventFecha() == null) {
            evento.setEventFecha(LocalDate.now());
        }
        
        return eventosRepository.save(evento);
    }

    /**
     * Actualizar evento existente
     */
    @Transactional
    public Eventos actualizar(Long id, Eventos eventoActualizado) {
        Eventos eventoExistente = eventosRepository.findById(id)
            .orElseThrow(() -> new EventoNoEncontradoException(
                "Evento con ID " + id + " no encontrado"
            ));
        
        validarEvento(eventoActualizado);
        
        // Actualizar campos
        eventoExistente.setEventFecha(eventoActualizado.getEventFecha());
        eventoExistente.setEventDescri(eventoActualizado.getEventDescri());
        eventoExistente.setEventImpacto(eventoActualizado.getEventImpacto());
        eventoExistente.setEventosNivel(eventoActualizado.getEventosNivel());
        
        return eventosRepository.save(eventoExistente);
    }

    /**
     * Eliminar evento
     */
    @Transactional
    public void eliminar(Long id) {
        Eventos evento = eventosRepository.findById(id)
            .orElseThrow(() -> new EventoNoEncontradoException(
                "Evento con ID " + id + " no encontrado"
            ));
        
        eventosRepository.delete(evento);
    }

    // ==================== BÚSQUEDAS POR ACTIVO ====================

    /**
     * Buscar eventos por activo
     */
    public List<Eventos> buscarPorActivo(Long activoId) {
        return eventosRepository.findByActivo_ActivoIdOrderByEventFechaDesc(activoId);
    }

    /**
     * Buscar eventos por activo con paginación
     */
    public Page<Eventos> buscarPorActivo(Long activoId, Pageable pageable) {
        return eventosRepository.findByActivo_ActivoId(activoId, pageable);
    }

    /**
     * Buscar eventos por activo con detalles
     */
    public List<Eventos> buscarPorActivoConDetalles(Long activoId) {
        return eventosRepository.findByActivoIdWithDetails(activoId);
    }

    /**
     * Obtener últimos N eventos de un activo
     */
    public List<Eventos> obtenerUltimosEventosDeActivo(Long activoId, int cantidad) {
        return eventosRepository.findTop5ByActivo_ActivoIdOrderByEventFechaDesc(
            activoId, 
            Pageable.ofSize(cantidad)
        );
    }

    // ==================== BÚSQUEDAS POR NIVEL ====================

    /**
     * Buscar eventos por nivel
     */
    public List<Eventos> buscarPorNivel(Long nivelId) {
        return eventosRepository.findByEventosNivel_NivelId(nivelId);
    }

    /**
     * Buscar eventos por nivel con paginación
     */
    public Page<Eventos> buscarPorNivel(Long nivelId, Pageable pageable) {
        return eventosRepository.findByEventosNivel_NivelId(nivelId, pageable);
    }

    /**
     * Buscar eventos críticos
     */
    public List<Eventos> buscarCriticos() {
        return eventosRepository.findEventosCriticos();
    }

    // ==================== BÚSQUEDAS POR FECHA ====================

    /**
     * Buscar eventos por rango de fechas
     */
    public List<Eventos> buscarPorFechas(LocalDate fechaInicio, LocalDate fechaFin) {
        return eventosRepository.findByFechaBetween(fechaInicio, fechaFin);
    }

    /**
     * Buscar eventos de hoy
     */
    public List<Eventos> buscarDeHoy() {
        return eventosRepository.findEventosDeHoy(LocalDate.now());
    }

    /**
     * Buscar eventos de la última semana
     */
    public List<Eventos> buscarUltimaSemana() {
        LocalDate hace7Dias = LocalDate.now().minusDays(7);
        return eventosRepository.findEventosUltimaSemana(hace7Dias);
    }

    /**
     * Buscar eventos del mes actual
     */
    public List<Eventos> buscarDelMesActual() {
        LocalDate hoy = LocalDate.now();
        LocalDate primerDiaMes = hoy.withDayOfMonth(1);
        LocalDate ultimoDiaMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
        return buscarPorFechas(primerDiaMes, ultimoDiaMes);
    }

    // ==================== BÚSQUEDAS POR DESCRIPCIÓN ====================

    /**
     * Buscar eventos por descripción
     */
    public List<Eventos> buscarPorDescripcion(String texto) {
        return eventosRepository.findByEventDescriContaining(texto);
    }

    /**
     * Buscar eventos con impacto registrado
     */
    public List<Eventos> buscarConImpacto() {
        return eventosRepository.findEventosConImpacto();
    }

    /**
     * Buscar eventos sin impacto registrado
     */
    public List<Eventos> buscarSinImpacto() {
        return eventosRepository.findEventosSinImpacto();
    }

    // ==================== BÚSQUEDAS COMBINADAS ====================

    /**
     * Buscar eventos por activo y nivel
     */
    public List<Eventos> buscarPorActivoYNivel(Long activoId, Long nivelId) {
        return eventosRepository.findByActivo_ActivoIdAndEventosNivel_NivelId(activoId, nivelId);
    }

    // ==================== ESTADÍSTICAS ====================

    /**
     * Contar eventos por nivel
     */
    public List<Object[]> contarPorNivel() {
        return eventosRepository.countEventosPorNivel();
    }

    /**
     * Contar eventos por activo
     */
    public List<Object[]> contarPorActivo() {
        return eventosRepository.countEventosPorActivo();
    }

    /**
     * Contar eventos por mes (año actual)
     */
    public List<Object[]> contarPorMes() {
        int anioActual = LocalDate.now().getYear();
        return eventosRepository.countEventosPorMes(anioActual);
    }

    /**
     * Contar eventos de un activo
     */
    public Long contarPorActivoId(Long activoId) {
        return eventosRepository.countEventosPorActivoId(activoId);
    }

    /**
     * Contar eventos críticos
     */
    public Long contarCriticos() {
        return eventosRepository.countEventosCriticos();
    }

    /**
     * Obtener últimos eventos globales
     */
    public List<Eventos> obtenerUltimos(int cantidad) {
        return eventosRepository.findTop10ByOrderByEventFechaDesc();
    }

    // ==================== VALIDACIONES ====================

    /**
     * Validar datos de evento
     */
    private void validarEvento(Eventos evento) {
        if (evento.getActivo() == null) {
            throw new EventoInvalidoException("El activo es obligatorio");
        }
        
        if (evento.getEventosNivel() == null) {
            throw new EventoInvalidoException("El nivel del evento es obligatorio");
        }
        
        if (evento.getEventDescri() == null || evento.getEventDescri().trim().isEmpty()) {
            throw new EventoInvalidoException("La descripción del evento es obligatoria");
        }
    }

    // ==================== MÉTODOS PARA OBTENER CATÁLOGOS ====================

    /**
     * Obtener todos los niveles de eventos
     */
    public List<EventosNiveles> obtenerTodosNiveles() {
        return nivelesRepository.findAll();
    }

    /**
     * Obtener todos los activos
     */
    public List<Activo> obtenerTodosActivos() {
        return activoRepository.findAll();
    }

    // ==================== EXCEPCIONES PERSONALIZADAS ====================

    public static class EventoNoEncontradoException extends RuntimeException {
        public EventoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class EventoInvalidoException extends RuntimeException {
        public EventoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }
}