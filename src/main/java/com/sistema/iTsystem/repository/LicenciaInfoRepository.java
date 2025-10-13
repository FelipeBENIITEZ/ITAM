package com.sistema.iTsystem.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.LicenciaInfo;
import com.sistema.iTsystem.model.LicenciasEstados;
import com.sistema.iTsystem.model.LicenciasTipo;
import com.sistema.iTsystem.model.SoftwareInfo;

@Repository
public interface LicenciaInfoRepository extends JpaRepository<LicenciaInfo, Long> {
    
    // Buscar por software
    List<LicenciaInfo> findBySoftwareInfo(SoftwareInfo softwareInfo);
    
    // Buscar por software ID
    List<LicenciaInfo> findBySoftwareInfo_SftId(Long sftId);
    
    // Buscar por estado
    List<LicenciaInfo> findByLicenciaEstado(LicenciasEstados estado);
    
    // Buscar por estado ID
    List<LicenciaInfo> findByLicenciaEstado_LicEstadoId(Long estadoId);
    
    // Buscar por tipo
    List<LicenciaInfo> findByLicenciaTipo(LicenciasTipo tipo);
    
    // Buscar por tipo ID
    List<LicenciaInfo> findByLicenciaTipo_LicTipoId(Long tipoId);
    
    // Buscar licencias vencidas
    @Query("SELECT l FROM LicenciaInfo l WHERE l.licenciaFin < :fecha")
    List<LicenciaInfo> findLicenciasVencidas(@Param("fecha") LocalDate fecha);
    
    // Buscar licencias próximas a vencer (30 días o menos)
    @Query("SELECT l FROM LicenciaInfo l WHERE l.licenciaFin BETWEEN :fechaInicio AND :fechaFin")
    List<LicenciaInfo> findLicenciasProximasAVencer(@Param("fechaInicio") LocalDate fechaInicio, 
                                                      @Param("fechaFin") LocalDate fechaFin);
    
    // Buscar licencias activas
    @Query("SELECT l FROM LicenciaInfo l WHERE l.licenciaFin IS NULL OR l.licenciaFin >= :fecha")
    List<LicenciaInfo> findLicenciasActivas(@Param("fecha") LocalDate fecha);
    
    // Buscar licencias con cupos disponibles
    @Query("SELECT l FROM LicenciaInfo l WHERE l.licenciaUsos < l.licenciaCupos")
    List<LicenciaInfo> findLicenciasConCuposDisponibles();
    
    // Buscar licencias sin cupos disponibles
    @Query("SELECT l FROM LicenciaInfo l WHERE l.licenciaUsos >= l.licenciaCupos")
    List<LicenciaInfo> findLicenciasSinCupos();
    
    // Query optimizada con todas las relaciones
    @Query("SELECT l FROM LicenciaInfo l " +
           "LEFT JOIN FETCH l.softwareInfo s " +
           "LEFT JOIN FETCH l.licenciaEstado " +
           "LEFT JOIN FETCH l.licenciaTipo " +
           "WHERE l.licenciaId = :licenciaId")
    LicenciaInfo findByIdWithDetails(@Param("licenciaId") Long licenciaId);
    
    // Contar licencias por estado
    @Query("SELECT l.licenciaEstado.licEstadoNom, COUNT(l) FROM LicenciaInfo l " +
           "GROUP BY l.licenciaEstado.licEstadoNom")
    List<Object[]> countLicenciasPorEstado();
    
    // Suma total de costos de licencias
    @Query("SELECT COALESCE(SUM(l.licenciaCosto), 0) FROM LicenciaInfo l WHERE l.licenciaCosto IS NOT NULL")
    java.math.BigDecimal sumTotalCostoLicencias();
    
    // Contar total de cupos disponibles
    @Query("SELECT SUM(l.licenciaCupos - l.licenciaUsos) FROM LicenciaInfo l")
    Long countTotalCuposDisponibles();
}