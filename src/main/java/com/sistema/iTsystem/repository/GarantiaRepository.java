package com.sistema.iTsystem.repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Garantia;
import com.sistema.iTsystem.model.HardwareInfo;

@Repository
public interface GarantiaRepository extends JpaRepository<Garantia, Long> {

    /**
     * Busca la garantia de un hardware específico
     */
    Optional<Garantia> findByHardwareInfo(HardwareInfo hardwareInfo);

    /**
     * Busca la garantia por el ID del hardware
     */
    @Query("SELECT g FROM Garantia g WHERE g.hardwareInfo.hwId = :hwId")
    Optional<Garantia> findByHardwareInfoId(Long hwId);

    /**
     * Encuentra todas las garantias con un estado especifico
     */
    @Query("SELECT g FROM Garantia g WHERE g.garantVigencia = :estado")
    List<Garantia> findByEstado(String estado);

    /**
     * Encuentra garantías próximas a vencer (útil para alertas)
     */
    @Query("SELECT g FROM Garantia g WHERE g.garantVigencia = 'Proxima a vencer'")
    List<Garantia> findProximasAVencer();

    /**
     * Encuentra garantias que vencen en un rango de fechas
     */
    @Query("SELECT g FROM Garantia g WHERE g.garantFechaFin BETWEEN :fechaInicio AND :fechaFin")
    List<Garantia> findByFechaFinBetween(LocalDate fechaInicio, LocalDate fechaFin);

    /**
     * Encuentra garantias activas
     */
    @Query("SELECT g FROM Garantia g WHERE g.garantVigencia IN ('Activa', 'Proxima a vencer')")
    List<Garantia> findGarantiasVigentes();

    /**
     * Cuenta garantias por estado
     */
    @Query("SELECT g.garantVigencia, COUNT(g) FROM Garantia g GROUP BY g.garantVigencia")
    List<Object[]> contarPorEstado();
}