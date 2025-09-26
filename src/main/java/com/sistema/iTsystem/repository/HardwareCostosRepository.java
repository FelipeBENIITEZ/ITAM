package com.sistema.iTsystem.repository;

import com.sistema.iTsystem.model.HardwareCostos;
import com.sistema.iTsystem.model.PresupuestoAreas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface HardwareCostosRepository extends JpaRepository<HardwareCostos, Long> {
    
    //Buscar costos por hardware ID
    Optional<HardwareCostos> findByHwId(Long hwId);
    
    //Buscar todos los costos por presupuesto
    List<HardwareCostos> findByPresupuesto(PresupuestoAreas presupuesto);
    
    // Buscar costos por ID del presupuesto
    List<HardwareCostos> findByPresupuesto_PresId(Long presId);
    
    // Buscar hardware vendido
    @Query("SELECT hc FROM HardwareCostos hc WHERE hc.hwValorVenta > 0")
    List<HardwareCostos> findHardwareVendido();
    
    // Buscar hardware no vendido
    @Query("SELECT hc FROM HardwareCostos hc WHERE hc.hwValorVenta = 0 OR hc.hwValorVenta IS NULL")
    List<HardwareCostos> findHardwareNoVendido();
    
    // Calcular total gastado por presupuesto
    @Query("SELECT COALESCE(SUM(hc.hwValorCompra), 0) FROM HardwareCostos hc WHERE hc.presupuesto.presId = :presId")
    BigDecimal calcularTotalGastadoPorPresupuesto(@Param("presId") Long presId);
    
    // Calcular total de ingresos por ventas en un presupuesto
    @Query("SELECT COALESCE(SUM(hc.hwValorVenta), 0) FROM HardwareCostos hc WHERE hc.presupuesto.presId = :presId AND hc.hwValorVenta > 0")
    BigDecimal calcularTotalIngresosVentasPorPresupuesto(@Param("presId") Long presId);
    
    // Buscar hardware por rango de precios
    @Query("SELECT hc FROM HardwareCostos hc WHERE hc.hwValorCompra BETWEEN :minPrecio AND :maxPrecio")
    List<HardwareCostos> findByRangoPrecios(@Param("minPrecio") BigDecimal minPrecio, 
                                           @Param("maxPrecio") BigDecimal maxPrecio);
    
    // Buscar los hardware mas costosos
    List<HardwareCostos> findTop10ByOrderByHwValorCompraDesc();
    
    // Verificar si existe informacion de costos para un hardware
    boolean existsByHwId(Long hwId);
    
    // Buscar costos con ganancia/perdida (vendidos)
    @Query("SELECT hc FROM HardwareCostos hc WHERE hc.hwValorVenta > 0 " +
           "ORDER BY (hc.hwValorVenta - hc.hwValorCompra) DESC")
    List<HardwareCostos> findHardwareConGananciaPerdida();
}