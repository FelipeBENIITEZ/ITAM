package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.HardwareInfo;

@Repository
public interface HardwareInfoRepository extends JpaRepository<HardwareInfo, Long> {
    
    // Buscar hardware info por activo ID
    Optional<HardwareInfo> findByActivo_ActivoId(Long activoId);
    
    // Buscar por serial
    Optional<HardwareInfo> findByHwSerialNum(String serialNum);
    
    // Query para obtener hardware info completo con todas las relaciones
    @Query("SELECT h FROM HardwareInfo h " +
           "LEFT JOIN FETCH h.modelo m " +
           "LEFT JOIN FETCH m.marca " +
           "LEFT JOIN FETCH h.garantia " +
           "WHERE h.activo.activoId = :activoId")
    Optional<HardwareInfo> findByActivoIdWithDetails(@Param("activoId") Long activoId);
}