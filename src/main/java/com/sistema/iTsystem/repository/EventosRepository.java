package com.sistema.iTsystem.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.Eventos;
import com.sistema.iTsystem.model.EventosNiveles;

@Repository
public interface EventosRepository extends JpaRepository<Eventos, Long> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    
    // Buscar eventos por activo
    List<Eventos> findByActivo(Activo activo);
    
    // Buscar eventos por activo ID
    List<Eventos> findByActivo_ActivoId(Long activoId);
    
    // Buscar eventos por activo ID ordenados por fecha descendente
    List<Eventos> findByActivo_ActivoIdOrderByEventFechaDesc(Long activoId);
    
    // Buscar eventos por nivel
    List<Eventos> findByEventosNivel(EventosNiveles nivel);
    
    // Buscar eventos por nivel ID
    List<Eventos> findByEventosNivel_NivelId(Long nivelId);
    
    // ==================== BÚSQUEDAS CON PAGINACIÓN ====================
    
    // Buscar eventos por activo con paginación
    Page<Eventos> findByActivo_ActivoId(Long activoId, Pageable pageable);
    
    // Buscar eventos por nivel con paginación
    Page<Eventos> findByEventosNivel_NivelId(Long nivelId, Pageable pageable);
    
    // Todos los eventos con paginación ordenados por fecha
    Page<Eventos> findAllByOrderByEventFechaDesc(Pageable pageable);
    
    // ==================== BÚSQUEDAS AVANZADAS ====================
    
    // Buscar eventos por rango de fechas
    @Query("SELECT e FROM Eventos e WHERE e.eventFecha BETWEEN :fechaInicio AND :fechaFin ORDER BY e.eventFecha DESC")
    List<Eventos> findByFechaBetween(@Param("fechaInicio") LocalDate fechaInicio, 
                                     @Param("fechaFin") LocalDate fechaFin);
    
    // Buscar eventos críticos (nivel específico)
    @Query("SELECT e FROM Eventos e WHERE e.eventosNivel.nivelNom = 'Crítico' ORDER BY e.eventFecha DESC")
    List<Eventos> findEventosCriticos();
    
    // Buscar eventos con impacto
    @Query("SELECT e FROM Eventos e WHERE e.eventImpacto IS NOT NULL AND e.eventImpacto != '' ORDER BY e.eventFecha DESC")
    List<Eventos> findEventosConImpacto();
    
    // Buscar eventos sin impacto registrado
    @Query("SELECT e FROM Eventos e WHERE e.eventImpacto IS NULL OR e.eventImpacto = '' ORDER BY e.eventFecha DESC")
    List<Eventos> findEventosSinImpacto();
    
    // Buscar por descripción
    @Query("SELECT e FROM Eventos e WHERE LOWER(e.eventDescri) LIKE LOWER(CONCAT('%', :texto, '%')) ORDER BY e.eventFecha DESC")
    List<Eventos> findByEventDescriContaining(@Param("texto") String texto);
    
    // Buscar eventos de hoy
    @Query("SELECT e FROM Eventos e WHERE e.eventFecha = :fecha ORDER BY e.createdAt DESC")
    List<Eventos> findEventosDeHoy(@Param("fecha") LocalDate fecha);
    
    // Buscar eventos de la última semana
    @Query("SELECT e FROM Eventos e WHERE e.eventFecha >= :fechaInicio ORDER BY e.eventFecha DESC")
    List<Eventos> findEventosUltimaSemana(@Param("fechaInicio") LocalDate fechaInicio);
    
    // Buscar eventos por activo y nivel
    List<Eventos> findByActivo_ActivoIdAndEventosNivel_NivelId(Long activoId, Long nivelId);
    
    // ==================== QUERIES OPTIMIZADAS ====================
    
    // Query optimizada con todas las relaciones
    @Query("SELECT e FROM Eventos e " +
           "LEFT JOIN FETCH e.activo a " +
           "LEFT JOIN FETCH a.departamento " +
           "LEFT JOIN FETCH a.estado " +
           "LEFT JOIN FETCH e.eventosNivel " +
           "WHERE e.eventId = :eventId")
    Eventos findByIdWithDetails(@Param("eventId") Long eventId);
    
    // Obtener eventos de un activo con detalles
    @Query("SELECT e FROM Eventos e " +
           "LEFT JOIN FETCH e.activo a " +
           "LEFT JOIN FETCH e.eventosNivel " +
           "WHERE a.activoId = :activoId " +
           "ORDER BY e.eventFecha DESC")
    List<Eventos> findByActivoIdWithDetails(@Param("activoId") Long activoId);
    
    // ==================== ESTADÍSTICAS ====================
    
    // Contar eventos por nivel
    @Query("SELECT e.eventosNivel.nivelNom, COUNT(e) FROM Eventos e " +
           "GROUP BY e.eventosNivel.nivelNom " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> countEventosPorNivel();
    
    // Contar eventos por activo
    @Query("SELECT a.activoNom, COUNT(e) FROM Eventos e " +
           "JOIN e.activo a " +
           "GROUP BY a.activoNom " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> countEventosPorActivo();
    
    // Contar eventos por mes (año actual)
    @Query("SELECT MONTH(e.eventFecha), COUNT(e) FROM Eventos e " +
           "WHERE YEAR(e.eventFecha) = :anio " +
           "GROUP BY MONTH(e.eventFecha) " +
           "ORDER BY MONTH(e.eventFecha)")
    List<Object[]> countEventosPorMes(@Param("anio") int anio);
    
    // Contar eventos de un activo
    @Query("SELECT COUNT(e) FROM Eventos e WHERE e.activo.activoId = :activoId")
    Long countEventosPorActivoId(@Param("activoId") Long activoId);
    
    // Contar eventos críticos
    @Query("SELECT COUNT(e) FROM Eventos e WHERE e.eventosNivel.nivelNom = 'Crítico'")
    Long countEventosCriticos();
    
    // Obtener últimos eventos (top N)
    List<Eventos> findTop10ByOrderByEventFechaDesc();
    
    // Obtener eventos recientes de un activo
    @Query("SELECT e FROM Eventos e " +
           "WHERE e.activo.activoId = :activoId " +
           "ORDER BY e.eventFecha DESC")
    List<Eventos> findTop5ByActivo_ActivoIdOrderByEventFechaDesc(@Param("activoId") Long activoId, Pageable pageable);
}