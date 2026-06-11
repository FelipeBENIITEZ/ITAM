package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Modelo;

@Repository
public interface HardwareInfoRepository extends JpaRepository<HardwareInfo, Long> {

    Optional<HardwareInfo> findByActivo_ActivoId(Long activoId);

    Optional<HardwareInfo> findByHwSerialNum(String serialNum);

    boolean existsByHwSerialNum(String serialNum);

    @Query("SELECT h FROM HardwareInfo h " +
           "LEFT JOIN FETCH h.modelo m " +
           "LEFT JOIN FETCH m.marca " +
           "LEFT JOIN FETCH h.garantias " +
           "LEFT JOIN FETCH h.activo a " +
           "LEFT JOIN FETCH a.proveedor " +
           "WHERE h.activo.activoId = :activoId")
    Optional<HardwareInfo> findByActivoIdWithDetails(@Param("activoId") Long activoId);

    @Query("SELECT h FROM HardwareInfo h " +
           "LEFT JOIN FETCH h.modelo m " +
           "LEFT JOIN FETCH m.marca " +
           "LEFT JOIN FETCH h.garantias " +
           "LEFT JOIN FETCH h.activo a " +
           "LEFT JOIN FETCH a.proveedor " +
           "WHERE h.hwId = :hwId")
    Optional<HardwareInfo> findByIdWithDetails(@Param("hwId") Long hwId);

    List<HardwareInfo> findByModelo(Modelo modelo);

    List<HardwareInfo> findByModelo_ModelId(Long modeloId);

    @Query("SELECT a.proveedor.provNom, COUNT(h) FROM HardwareInfo h " +
           "JOIN h.activo a " +
           "WHERE a.proveedor IS NOT NULL " +
           "GROUP BY a.proveedor.provNom " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> countHardwarePorProveedor();

    @Query("SELECT m.modelNom, COUNT(h) FROM HardwareInfo h " +
           "JOIN h.modelo m " +
           "GROUP BY m.modelNom " +
           "ORDER BY COUNT(h) DESC")
    List<Object[]> countHardwarePorModelo();

    @Query("SELECT h FROM HardwareInfo h JOIN h.activo a WHERE a.proveedor IS NULL")
    List<HardwareInfo> findHardwareSinProveedor();
}
