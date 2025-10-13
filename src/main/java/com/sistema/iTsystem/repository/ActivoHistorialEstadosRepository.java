package com.sistema.iTsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.ActivoHistorialEstados;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.Usuario;

@Repository
public interface ActivoHistorialEstadosRepository extends JpaRepository<ActivoHistorialEstados, Long> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    
    // Buscar historial por activo
    List<ActivoHistorialEstados> findByActivo(Activo activo);
    
    // Buscar historial por activo ID ordenado por fecha
    List<ActivoHistorialEstados> findByActivo_ActivoIdOrderByFechaCambioDesc(Long activoId);
    
    // Buscar historial por activo ID ordenado cronológicamente
    List<ActivoHistorialEstados> findByActivo_ActivoIdOrderByFechaCambioAsc(Long activoId);
    
    // Buscar cambios realizados por un usuario
    List<ActivoHistorialEstados> findByUsuario(Usuario usuario);
    
    // Buscar cambios realizados por usuario ID
    List<ActivoHistorialEstados> findByUsuario_UsuId(Long usuarioId);
    
    // Buscar cambios a un estado específico
    List<ActivoHistorialEstados> findByEstadoNuevo(EstadoActivo estado);
    
    // Buscar cambios desde un estado específico
    List<ActivoHistorialEstados> findByEstadoAnterior(EstadoActivo estado);
    
    // ==================== BÚSQUEDAS CON PAGINACIÓN ====================
    
    // Buscar historial por activo con paginación
    Page<ActivoHistorialEstados> findByActivo_ActivoId(Long activoId, Pageable pageable);
    
    // Buscar cambios de un usuario con paginación
    Page<ActivoHistorialEstados> findByUsuario_UsuId(Long usuarioId, Pageable pageable);
    
    // Todos los cambios con paginación
    Page<ActivoHistorialEstados> findAllByOrderByFechaCambioDesc(Pageable pageable);
    
    // ==================== BÚSQUEDAS AVANZADAS ====================
    
    // Obtener último cambio de estado de un activo
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.activo.activoId = :activoId " +
           "ORDER BY h.fechaCambio DESC " +
           "LIMIT 1")
    ActivoHistorialEstados findUltimoCambioByActivoId(@Param("activoId") Long activoId);
    
    // Obtener primer cambio de estado de un activo (registro inicial)
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.activo.activoId = :activoId " +
           "ORDER BY h.fechaCambio ASC " +
           "LIMIT 1")
    ActivoHistorialEstados findPrimerCambioByActivoId(@Param("activoId") Long activoId);
    
    // Buscar cambios por rango de fechas
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.fechaCambio BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByFechaCambioBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                                           @Param("fechaFin") LocalDateTime fechaFin);
    
    // Buscar cambios de un activo en un rango de fechas
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.activo.activoId = :activoId " +
           "AND h.fechaCambio BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByActivoAndFechaBetween(@Param("activoId") Long activoId,
                                                              @Param("fechaInicio") LocalDateTime fechaInicio,
                                                              @Param("fechaFin") LocalDateTime fechaFin);
    
    // Buscar transiciones específicas (de estado X a estado Y)
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.estadoAnterior.estadoId = :estadoAnteriorId " +
           "AND h.estadoNuevo.estadoId = :estadoNuevoId " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findTransiciones(@Param("estadoAnteriorId") Long estadoAnteriorId,
                                                   @Param("estadoNuevoId") Long estadoNuevoId);
    
    // Buscar registros iniciales (sin estado anterior)
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.estadoAnterior IS NULL ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findRegistrosIniciales();
    
    // Buscar cambios con motivo específico
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE LOWER(h.motivo) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByMotivoContaining(@Param("texto") String texto);
    
    // Buscar cambios sin motivo
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.motivo IS NULL OR h.motivo = '' " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosSinMotivo();
    
    // Buscar cambios de hoy
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE DATE(h.fechaCambio) = CURRENT_DATE " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosDeHoy();
    
    // Buscar cambios de la última semana
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.fechaCambio >= :fechaInicio " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosUltimaSemana(@Param("fechaInicio") LocalDateTime fechaInicio);
    
    // ==================== QUERIES OPTIMIZADAS ====================
    
    // Query optimizada con todas las relaciones
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "LEFT JOIN FETCH h.activo a " +
           "LEFT JOIN FETCH a.departamento " +
           "LEFT JOIN FETCH a.categoria " +
           "LEFT JOIN FETCH h.estadoAnterior " +
           "LEFT JOIN FETCH h.estadoNuevo " +
           "LEFT JOIN FETCH h.usuario u " +
           "LEFT JOIN FETCH u.persona " +
           "WHERE h.historialId = :historialId")
    ActivoHistorialEstados findByIdWithDetails(@Param("historialId") Long historialId);
    
    // Obtener historial completo de un activo con detalles
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "LEFT JOIN FETCH h.estadoAnterior " +
           "LEFT JOIN FETCH h.estadoNuevo " +
           "LEFT JOIN FETCH h.usuario u " +
           "LEFT JOIN FETCH u.persona " +
           "WHERE h.activo.activoId = :activoId " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByActivoIdWithDetails(@Param("activoId") Long activoId);
    
    // ==================== ESTADÍSTICAS Y REPORTES ====================
    
    // Contar cambios por activo
    @Query("SELECT COUNT(h) FROM ActivoHistorialEstados h WHERE h.activo.activoId = :activoId")
    Long countCambiosPorActivo(@Param("activoId") Long activoId);
    
    // Contar cambios por usuario
    @Query("SELECT COUNT(h) FROM ActivoHistorialEstados h WHERE h.usuario.usuId = :usuarioId")
    Long countCambiosPorUsuario(@Param("usuarioId") Long usuarioId);
    
    // Contar transiciones a un estado
    @Query("SELECT COUNT(h) FROM ActivoHistorialEstados h WHERE h.estadoNuevo.estadoId = :estadoId")
    Long countTransicionesAEstado(@Param("estadoId") Long estadoId);
    
    // Estadísticas de transiciones por estado
    @Query("SELECT h.estadoNuevo.estadoNom, COUNT(h) FROM ActivoHistorialEstados h " +
           "GROUP BY h.estadoNuevo.estadoNom " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> countTransicionesPorEstado();
    
    // Transiciones más comunes (de X a Y)
    @Query("SELECT h.estadoAnterior.estadoNom, h.estadoNuevo.estadoNom, COUNT(h) " +
           "FROM ActivoHistorialEstados h " +
           "WHERE h.estadoAnterior IS NOT NULL " +
           "GROUP BY h.estadoAnterior.estadoNom, h.estadoNuevo.estadoNom " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> findTransicionesMasComunes();
    
    // Usuarios más activos en cambios de estado
    @Query("SELECT u.usuLogin, COUNT(h) FROM ActivoHistorialEstados h " +
           "JOIN h.usuario u " +
           "GROUP BY u.usuLogin " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> findUsuariosMasActivos();
    
    // Activos con más cambios de estado
    @Query("SELECT a.activoNom, COUNT(h) FROM ActivoHistorialEstados h " +
           "JOIN h.activo a " +
           "GROUP BY a.activoNom " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> findActivosConMasCambios();
    
    // Cambios por mes (año específico)
    @Query("SELECT MONTH(h.fechaCambio), COUNT(h) FROM ActivoHistorialEstados h " +
           "WHERE YEAR(h.fechaCambio) = :anio " +
           "GROUP BY MONTH(h.fechaCambio) " +
           "ORDER BY MONTH(h.fechaCambio)")
    List<Object[]> countCambiosPorMes(@Param("anio") int anio);
    
    // Promedio de cambios por activo
    @Query("SELECT AVG(cambios) FROM " +
           "(SELECT COUNT(h) as cambios FROM ActivoHistorialEstados h GROUP BY h.activo.activoId)")
    Double avgCambiosPorActivo();
    
    // Obtener últimos cambios globales
    List<ActivoHistorialEstados> findTop20ByOrderByFechaCambioDesc();
    
    // Obtener últimos N cambios de un activo
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "WHERE h.activo.activoId = :activoId " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findTopNByActivoId(@Param("activoId") Long activoId, Pageable pageable);
}