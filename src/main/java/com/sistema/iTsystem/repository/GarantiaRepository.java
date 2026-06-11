package com.sistema.iTsystem.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Garantia;
import com.sistema.iTsystem.model.HardwareInfo;

@Repository
public interface GarantiaRepository extends JpaRepository<Garantia, Long> {

    List<Garantia> findByHardwareInfo(HardwareInfo hardwareInfo);

    List<Garantia> findByHardwareInfo_HwId(Long hwId);

    @Query("SELECT g FROM Garantia g WHERE g.garanEstado = :estado")
    List<Garantia> findByEstado(String estado);

    @Query("SELECT g FROM Garantia g WHERE g.garanEstado = 'Proxima a vencer'")
    List<Garantia> findProximasAVencer();

    @Query("SELECT g FROM Garantia g WHERE g.garanFechaFin BETWEEN :fechaInicio AND :fechaFin")
    List<Garantia> findByFechaFinBetween(LocalDate fechaInicio, LocalDate fechaFin);

    @Query("SELECT g FROM Garantia g WHERE g.garanEstado IN ('Activa', 'Proxima a vencer')")
    List<Garantia> findGarantiasVigentes();

    @Query("SELECT g.garanEstado, COUNT(g) FROM Garantia g GROUP BY g.garanEstado")
    List<Object[]> contarPorEstado();
}
