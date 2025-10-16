package com.sistema.iTsystem.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.ActivoHistorialEstados;

@Repository
public interface ActivoHistorialEstadosRepository extends JpaRepository<ActivoHistorialEstados, Long> {

    /**
     * Buscar historial por activo
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.activo.activoId = :activoId ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByActivoId(@Param("activoId") Long activoId);

    /**
     * Buscar historial por activo con detalles (JOIN FETCH) - AGREGADO
     */
    @Query("SELECT h FROM ActivoHistorialEstados h " +
           "LEFT JOIN FETCH h.activo " +
           "LEFT JOIN FETCH h.estadoAnterior " +
           "LEFT JOIN FETCH h.estadoNuevo " +
           "LEFT JOIN FETCH h.usuario " +
           "WHERE h.activo.activoId = :activoId " +
           "ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByActivoIdWithDetails(@Param("activoId") Long activoId);

    /**
     * Buscar cambios de hoy
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE CAST(h.fechaCambio AS date) = CURRENT_DATE ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosDeHoy();

    /**
     * Buscar cambios por fecha
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE CAST(h.fechaCambio AS date) = :fecha ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosPorFecha(@Param("fecha") LocalDate fecha);

    /**
     * Buscar cambios entre fechas
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.fechaCambio BETWEEN :inicio AND :fin ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosEntreFechas(
        @Param("inicio") LocalDateTime inicio, 
        @Param("fin") LocalDateTime fin
    );

    /**
     * Buscar por estado anterior
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.estadoAnterior.estadoId = :estadoId ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByEstadoAnteriorId(@Param("estadoId") Long estadoId);

    /**
     * Buscar por estado nuevo
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.estadoNuevo.estadoId = :estadoId ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByEstadoNuevoId(@Param("estadoId") Long estadoId);

    /**
     * Buscar cambios por usuario
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.usuario.usuId = :usuarioId ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findByUsuarioId(@Param("usuarioId") Long usuarioId);

    /**
     * Contar cambios de un activo
     */
    @Query("SELECT COUNT(h) FROM ActivoHistorialEstados h WHERE h.activo.activoId = :activoId")
    Long countByActivoId(@Param("activoId") Long activoId);

    /**
     * Último cambio de un activo
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.activo.activoId = :activoId ORDER BY h.fechaCambio DESC LIMIT 1")
    ActivoHistorialEstados findUltimoCambio(@Param("activoId") Long activoId);
    
    /**
     * Buscar registros iniciales (sin estado anterior)
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.estadoAnterior IS NULL ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findRegistrosIniciales();
    
    /**
     * Buscar cambios de esta semana
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.fechaCambio >= :inicioSemana ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosDeLaSemana(@Param("inicioSemana") LocalDateTime inicioSemana);
    
    /**
     * Buscar cambios de este mes
     */
    @Query("SELECT h FROM ActivoHistorialEstados h WHERE h.fechaCambio >= :inicioMes ORDER BY h.fechaCambio DESC")
    List<ActivoHistorialEstados> findCambiosDelMes(@Param("inicioMes") LocalDateTime inicioMes);
}