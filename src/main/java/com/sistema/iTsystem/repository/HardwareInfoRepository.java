package com.sistema.iTsystem.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Modelo;
import com.sistema.iTsystem.model.Proveedores;

@Repository
public interface HardwareInfoRepository extends JpaRepository<HardwareInfo, Long> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    
    // Buscar hardware info por activo ID
    Optional<HardwareInfo> findByActivo_ActivoId(Long activoId);
    
    // Buscar por serial
    Optional<HardwareInfo> findByHwSerialNum(String serialNum);
    
    // Verificar si existe un serial
    boolean existsByHwSerialNum(String serialNum);
    
    // ==================== BÚSQUEDAS CON RELACIONES ====================
    
    // Query para obtener hardware info completo con todas las relaciones
    @Query("SELECT h FROM HardwareInfo h " +
           "LEFT JOIN FETCH h.modelo m " +
           "LEFT JOIN FETCH m.marca " +
           "LEFT JOIN FETCH h.garantia " +
           "LEFT JOIN FETCH h.proveedor " +  // ✅ NUEVO: agregar proveedor
           "WHERE h.activo.activoId = :activoId")
    Optional<HardwareInfo> findByActivoIdWithDetails(@Param("activoId") Long activoId);
    
    // ✅ NUEVO: Query optimizada para obtener hardware con ID
    @Query("SELECT h FROM HardwareInfo h " +
           "LEFT JOIN FETCH h.modelo m " +
           "LEFT JOIN FETCH m.marca " +
           "LEFT JOIN FETCH h.garantia " +
           "LEFT JOIN FETCH h.proveedor " +
           "WHERE h.hwId = :hwId")
    Optional<HardwareInfo> findByIdWithDetails(@Param("hwId") Long hwId);
    
    // ==================== BÚSQUEDAS POR RELACIONES ====================
    
    // Buscar por modelo
    List<HardwareInfo> findByModelo(Modelo modelo);
    
    // Buscar por modelo ID
    List<HardwareInfo> findByModelo_ModelId(Long modeloId);
    
    // ✅ NUEVO: Buscar por proveedor
    List<HardwareInfo> findByProveedor(Proveedores proveedor);
    
    // ✅ NUEVO: Buscar por proveedor ID
    List<HardwareInfo> findByProveedor_ProvId(Long provId);
    
    // ==================== BÚSQUEDAS POR VALOR ====================
    
    // ✅ NUEVO: Buscar hardware con valor de compra mayor a X
    @Query("SELECT h FROM HardwareInfo h WHERE h.hwValorCompra > :valorMinimo")
    List<HardwareInfo> findByValorCompraGreaterThan(@Param("valorMinimo") BigDecimal valorMinimo);
    
    // ✅ NUEVO: Buscar hardware con valor de compra entre X y Y
    @Query("SELECT h FROM HardwareInfo h WHERE h.hwValorCompra BETWEEN :valorMin AND :valorMax")
    List<HardwareInfo> findByValorCompraBetween(@Param("valorMin") BigDecimal valorMin, 
                                                  @Param("valorMax") BigDecimal valorMax);
    
    // ✅ NUEVO: Buscar hardware sin valor de compra registrado
    @Query("SELECT h FROM HardwareInfo h WHERE h.hwValorCompra IS NULL")
    List<HardwareInfo> findHardwareSinValorCompra();
    
    // ==================== ESTADÍSTICAS Y REPORTES ====================
    
    // ✅ NUEVO: Obtener suma total de valores de compra
    @Query("SELECT COALESCE(SUM(h.hwValorCompra), 0) FROM HardwareInfo h WHERE h.hwValorCompra IS NOT NULL")
    BigDecimal sumTotalValorCompra();
    
    // ✅ NUEVO: Obtener valor promedio de compra
    @Query("SELECT AVG(h.hwValorCompra) FROM HardwareInfo h WHERE h.hwValorCompra IS NOT NULL")
    BigDecimal avgValorCompra();
    
    // ✅ NUEVO: Contar hardware por proveedor
    @Query("SELECT h.proveedor.provNom, COUNT(h) FROM HardwareInfo h " +
           "WHERE h.proveedor IS NOT NULL " +
           "GROUP BY h.proveedor.provNom " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> countHardwarePorProveedor();
    
    // ✅ NUEVO: Contar hardware por modelo
    @Query("SELECT m.modelNom, COUNT(h) FROM HardwareInfo h " +
           "JOIN h.modelo m " +
           "GROUP BY m.modelNom " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> countHardwarePorModelo();
    
    // ✅ NUEVO: Obtener hardware sin proveedor asignado
    @Query("SELECT h FROM HardwareInfo h WHERE h.proveedor IS NULL")
    List<HardwareInfo> findHardwareSinProveedor();
    
    // ✅ NUEVO: Contar hardware sin valor de compra
    @Query("SELECT COUNT(h) FROM HardwareInfo h WHERE h.hwValorCompra IS NULL")
    long countHardwareSinValorCompra();
}