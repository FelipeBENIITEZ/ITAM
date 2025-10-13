package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.InstalacionLicencias;
import com.sistema.iTsystem.model.LicenciaInfo;

@Repository
public interface InstalacionLicenciasRepository extends JpaRepository<InstalacionLicencias, InstalacionLicencias.InstalacionLicenciasId> {
    
    // Buscar instalaciones por hardware
    List<InstalacionLicencias> findByHardwareInfo(HardwareInfo hardwareInfo);
    
    // Buscar instalaciones por hardware ID
    List<InstalacionLicencias> findByHardwareInfo_HwId(Long hwId);
    
    // Buscar instalaciones por licencia
    List<InstalacionLicencias> findByLicenciaInfo(LicenciaInfo licenciaInfo);
    
    // Buscar instalaciones por licencia ID
    List<InstalacionLicencias> findByLicenciaInfo_LicenciaId(Long licenciaId);
    
    // Verificar si existe instalación
    boolean existsByHardwareInfo_HwIdAndLicenciaInfo_LicenciaId(Long hwId, Long licenciaId);
    
    // Buscar instalación específica
    Optional<InstalacionLicencias> findByHardwareInfo_HwIdAndLicenciaInfo_LicenciaId(Long hwId, Long licenciaId);
    
    // Contar instalaciones por hardware
    @Query("SELECT COUNT(i) FROM InstalacionLicencias i WHERE i.hardwareInfo.hwId = :hwId")
    Long countByHardwareId(@Param("hwId") Long hwId);
    
    // Contar instalaciones por licencia
    @Query("SELECT COUNT(i) FROM InstalacionLicencias i WHERE i.licenciaInfo.licenciaId = :licenciaId")
    Long countByLicenciaId(@Param("licenciaId") Long licenciaId);
    
    // Query optimizada con relaciones
    @Query("SELECT i FROM InstalacionLicencias i " +
           "LEFT JOIN FETCH i.hardwareInfo h " +
           "LEFT JOIN FETCH i.licenciaInfo l " +
           "LEFT JOIN FETCH l.softwareInfo " +
           "WHERE h.hwId = :hwId")
    List<InstalacionLicencias> findByHardwareIdWithDetails(@Param("hwId") Long hwId);
}