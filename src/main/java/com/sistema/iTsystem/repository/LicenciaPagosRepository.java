package com.sistema.iTsystem.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.LicenciaInfo;
import com.sistema.iTsystem.model.LicenciaPagos;

@Repository
public interface LicenciaPagosRepository extends JpaRepository<LicenciaPagos, Long> {
    
    // Buscar pagos por licencia
    List<LicenciaPagos> findByLicenciaInfo(LicenciaInfo licenciaInfo);
    
    // Buscar pagos por licencia ID
    List<LicenciaPagos> findByLicenciaInfo_LicenciaId(Long licenciaId);
    
    // Buscar pagos por rango de fechas
    @Query("SELECT p FROM LicenciaPagos p WHERE p.pagoFecha BETWEEN :fechaInicio AND :fechaFin ORDER BY p.pagoFecha DESC")
    List<LicenciaPagos> findByFechaBetween(@Param("fechaInicio") LocalDate fechaInicio, 
                                           @Param("fechaFin") LocalDate fechaFin);
    
    // Suma total de pagos
    @Query("SELECT COALESCE(SUM(p.pagoCost), 0) FROM LicenciaPagos p")
    java.math.BigDecimal sumTotalPagos();
    
    // Suma de pagos por licencia
    @Query("SELECT COALESCE(SUM(p.pagoCost), 0) FROM LicenciaPagos p WHERE p.licenciaInfo.licenciaId = :licenciaId")
    java.math.BigDecimal sumPagosPorLicencia(@Param("licenciaId") Long licenciaId);
    
    // Obtener últimos pagos (top N)
    List<LicenciaPagos> findTop10ByOrderByPagoFechaDesc();
}