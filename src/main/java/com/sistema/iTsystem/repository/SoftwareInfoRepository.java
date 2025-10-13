package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Proveedores;
import com.sistema.iTsystem.model.SoftTipo;
import com.sistema.iTsystem.model.SoftwareInfo;

@Repository
public interface SoftwareInfoRepository extends JpaRepository<SoftwareInfo, Long> {
    
    // Buscar por activo ID
    Optional<SoftwareInfo> findByActivo_ActivoId(Long activoId);
    
    // Buscar por nombre
    List<SoftwareInfo> findBySftNomContainingIgnoreCase(String nombre);
    
    // Buscar por tipo de software
    List<SoftwareInfo> findBySoftTipo(SoftTipo softTipo);
    
    // Buscar por tipo de software ID
    List<SoftwareInfo> findBySoftTipo_SoftTipoId(Long softTipoId);
    
    // Buscar por proveedor
    List<SoftwareInfo> findByProveedor(Proveedores proveedor);
    
    // Buscar por proveedor ID
    List<SoftwareInfo> findByProveedor_ProvId(Long provId);
    
    // Buscar software sin proveedor
    @Query("SELECT s FROM SoftwareInfo s WHERE s.proveedor IS NULL")
    List<SoftwareInfo> findSoftwareSinProveedor();
    
    // Query optimizada con todas las relaciones
    @Query("SELECT s FROM SoftwareInfo s " +
           "LEFT JOIN FETCH s.activo a " +
           "LEFT JOIN FETCH s.proveedor " +
           "LEFT JOIN FETCH s.softTipo " +
           "WHERE s.sftId = :sftId")
    Optional<SoftwareInfo> findByIdWithDetails(@Param("sftId") Long sftId);
    
    // Contar software por tipo
    @Query("SELECT s.softTipo.softTipoNom, COUNT(s) FROM SoftwareInfo s " +
           "GROUP BY s.softTipo.softTipoNom " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> countSoftwarePorTipo();
    
    // Contar software por proveedor
    @Query("SELECT s.proveedor.provNom, COUNT(s) FROM SoftwareInfo s " +
           "WHERE s.proveedor IS NOT NULL " +
           "GROUP BY s.proveedor.provNom " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> countSoftwarePorProveedor();
}