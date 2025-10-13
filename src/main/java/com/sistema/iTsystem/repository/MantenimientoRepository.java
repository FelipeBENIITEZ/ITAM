package com.sistema.iTsystem.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Mantenimiento;
import com.sistema.iTsystem.model.MantenimientoTipo;

@Repository
public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    
    // Buscar mantenimientos por hardware
    List<Mantenimiento> findByHardwareInfo(HardwareInfo hardwareInfo);
    
    // Buscar mantenimientos por hardware ID
    List<Mantenimiento> findByHardwareInfo_HwId(Long hwId);
    
    // Buscar mantenimientos por hardware ID ordenados por fecha
    List<Mantenimiento> findByHardwareInfo_HwIdOrderByMantFechaIniDesc(Long hwId);
    
    // Buscar mantenimientos por tipo
    List<Mantenimiento> findByMantenimientoTipo(MantenimientoTipo tipo);
    
    // Buscar mantenimientos por tipo ID
    List<Mantenimiento> findByMantenimientoTipo_MantTipoId(Long tipoId);
    
    // ==================== BÚSQUEDAS CON PAGINACIÓN ====================
    
    // Buscar mantenimientos por hardware con paginación
    Page<Mantenimiento> findByHardwareInfo_HwId(Long hwId, Pageable pageable);
    
    // Buscar mantenimientos por tipo con paginación
    Page<Mantenimiento> findByMantenimientoTipo_MantTipoId(Long tipoId, Pageable pageable);
    
    // Todos los mantenimientos con paginación ordenados por fecha
    Page<Mantenimiento> findAllByOrderByMantFechaIniDesc(Pageable pageable);
    
    // ==================== BÚSQUEDAS AVANZADAS ====================
    
    // Buscar mantenimientos en curso (sin fecha fin o fecha fin >= hoy)
    @Query("SELECT m FROM Mantenimiento m WHERE m.mantFechaFin IS NULL OR m.mantFechaFin >= :fecha ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findMantenimientosEnCurso(@Param("fecha") LocalDate fecha);
    
    // Buscar mantenimientos finalizados
    @Query("SELECT m FROM Mantenimiento m WHERE m.mantFechaFin IS NOT NULL AND m.mantFechaFin < :fecha ORDER BY m.mantFechaFin DESC")
    List<Mantenimiento> findMantenimientosFinalizados(@Param("fecha") LocalDate fecha);
    
    // Buscar mantenimientos por rango de fechas (inicio)
    @Query("SELECT m FROM Mantenimiento m WHERE m.mantFechaIni BETWEEN :fechaInicio AND :fechaFin ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findByFechaInicioBetween(@Param("fechaInicio") LocalDate fechaInicio, 
                                                   @Param("fechaFin") LocalDate fechaFin);
    
    // Buscar mantenimientos con costo mayor a X
    @Query("SELECT m FROM Mantenimiento m WHERE m.mantCosto > :costoMinimo ORDER BY m.mantCosto DESC")
    List<Mantenimiento> findByMantCostoGreaterThan(@Param("costoMinimo") BigDecimal costoMinimo);
    
    // Buscar mantenimientos sin costo registrado
    @Query("SELECT m FROM Mantenimiento m WHERE m.mantCosto IS NULL ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findMantenimientosSinCosto();
    
    // Buscar mantenimientos con descripción
    @Query("SELECT m FROM Mantenimiento m WHERE LOWER(m.mantDescri) LIKE LOWER(CONCAT('%', :texto, '%')) ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findByMantDescriContaining(@Param("texto") String texto);
    
    // Buscar mantenimientos preventivos
    @Query("SELECT m FROM Mantenimiento m WHERE m.mantenimientoTipo.mantTipoNom = 'Preventivo' ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findMantenimientosPreventivos();
    
    // Buscar mantenimientos correctivos
    @Query("SELECT m FROM Mantenimiento m WHERE m.mantenimientoTipo.mantTipoNom = 'Correctivo' ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findMantenimientosCorrectivos();
    
    // Buscar mantenimientos del mes actual
    @Query("SELECT m FROM Mantenimiento m WHERE MONTH(m.mantFechaIni) = :mes AND YEAR(m.mantFechaIni) = :anio ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findMantenimientosDelMes(@Param("mes") int mes, @Param("anio") int anio);
    
    // Buscar por hardware y tipo
    List<Mantenimiento> findByHardwareInfo_HwIdAndMantenimientoTipo_MantTipoId(Long hwId, Long tipoId);
    
    // ==================== QUERIES OPTIMIZADAS ====================
    
    // Query optimizada con todas las relaciones
    @Query("SELECT m FROM Mantenimiento m " +
           "LEFT JOIN FETCH m.hardwareInfo h " +
           "LEFT JOIN FETCH h.activo a " +
           "LEFT JOIN FETCH h.modelo mo " +
           "LEFT JOIN FETCH mo.marca " +
           "LEFT JOIN FETCH m.mantenimientoTipo " +
           "WHERE m.mantId = :mantId")
    Mantenimiento findByIdWithDetails(@Param("mantId") Long mantId);
    
    // Obtener mantenimientos de un hardware con detalles
    @Query("SELECT m FROM Mantenimiento m " +
           "LEFT JOIN FETCH m.hardwareInfo h " +
           "LEFT JOIN FETCH m.mantenimientoTipo " +
           "WHERE h.hwId = :hwId " +
           "ORDER BY m.mantFechaIni DESC")
    List<Mantenimiento> findByHardwareIdWithDetails(@Param("hwId") Long hwId);
    
    // ==================== ESTADÍSTICAS ====================
    
    // Suma total de costos de mantenimiento
    @Query("SELECT COALESCE(SUM(m.mantCosto), 0) FROM Mantenimiento m WHERE m.mantCosto IS NOT NULL")
    BigDecimal sumTotalCostoMantenimientos();
    
    // Suma de costos de mantenimiento por hardware
    @Query("SELECT COALESCE(SUM(m.mantCosto), 0) FROM Mantenimiento m WHERE m.hardwareInfo.hwId = :hwId AND m.mantCosto IS NOT NULL")
    BigDecimal sumCostosPorHardware(@Param("hwId") Long hwId);
    
    // Promedio de costos de mantenimiento
    @Query("SELECT AVG(m.mantCosto) FROM Mantenimiento m WHERE m.mantCosto IS NOT NULL")
    BigDecimal avgCostoMantenimiento();
    
    // Contar mantenimientos por tipo
    @Query("SELECT m.mantenimientoTipo.mantTipoNom, COUNT(m) FROM Mantenimiento m " +
           "GROUP BY m.mantenimientoTipo.mantTipoNom " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> countMantenimientosPorTipo();
    
    // Contar mantenimientos por hardware
    @Query("SELECT h.hwSerialNum, COUNT(m) FROM Mantenimiento m " +
           "JOIN m.hardwareInfo h " +
           "GROUP BY h.hwSerialNum " +
           "ORDER BY COUNT(m) DESC")
    List<Object[]> countMantenimientosPorHardware();
    
    // Contar mantenimientos por mes (año actual)
    @Query("SELECT MONTH(m.mantFechaIni), COUNT(m) FROM Mantenimiento m " +
           "WHERE YEAR(m.mantFechaIni) = :anio " +
           "GROUP BY MONTH(m.mantFechaIni) " +
           "ORDER BY MONTH(m.mantFechaIni)")
    List<Object[]> countMantenimientosPorMes(@Param("anio") int anio);
    
    // Contar mantenimientos en curso
    @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.mantFechaFin IS NULL OR m.mantFechaFin >= :fecha")
    Long countMantenimientosEnCurso(@Param("fecha") LocalDate fecha);
    
    // Contar mantenimientos de un hardware
    @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.hardwareInfo.hwId = :hwId")
    Long countMantenimientosPorHardwareId(@Param("hwId") Long hwId);
    
    // Obtener últimos mantenimientos (top N)
    List<Mantenimiento> findTop10ByOrderByMantFechaIniDesc();
    
    // Obtener mantenimientos más costosos
    List<Mantenimiento> findTop10ByOrderByMantCostoDesc();
    
    // Contar mantenimientos sin costo
    @Query("SELECT COUNT(m) FROM Mantenimiento m WHERE m.mantCosto IS NULL")
    Long countMantenimientosSinCosto();
}
