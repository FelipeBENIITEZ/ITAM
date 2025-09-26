package com.sistema.iTsystem.repository;

import com.sistema.iTsystem.model.Departamentos;
import com.sistema.iTsystem.model.PresupuestoAreas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PresupuestoAreasRepository extends JpaRepository<PresupuestoAreas, Long> {
    
    // Buscar presupuestos por departamento
    List<PresupuestoAreas> findByDepartamento(Departamentos departamento);
    
    // Buscar presupuestos por departamento ID
    List<PresupuestoAreas> findByDepartamento_DeptId(Long deptId);
    
    // Buscar presupuestos vigentes
    @Query("SELECT p FROM PresupuestoAreas p WHERE p.presIniVigencia <= :fecha AND p.presFinVigencia >= :fecha")
    List<PresupuestoAreas> findPresupuestosVigentes(@Param("fecha") LocalDate fecha);
    
    // Buscar presupuesto vigente por departamento
    @Query("SELECT p FROM PresupuestoAreas p WHERE p.departamento.deptId = :deptId " +
           "AND p.presIniVigencia <= :fecha AND p.presFinVigencia >= :fecha")
    Optional<PresupuestoAreas> findPresupuestoVigentePorDepartamento(@Param("deptId") Long deptId, 
                                                                     @Param("fecha") LocalDate fecha);
    
    // Buscar presupuestos con disponibilidad
    @Query("SELECT p FROM PresupuestoAreas p WHERE (p.presAsignado - p.presUsado) > 0")
    List<PresupuestoAreas> findPresupuestosConDisponibilidad();
    
    // Buscar presupuestos vigentes con disponibilidad por departamento
    @Query("SELECT p FROM PresupuestoAreas p WHERE p.departamento.deptId = :deptId " +
           "AND p.presIniVigencia <= :fecha AND p.presFinVigencia >= :fecha " +
           "AND (p.presAsignado - p.presUsado) > 0")
    List<PresupuestoAreas> findPresupuestosVigentesConDisponibilidadPorDepartamento(
            @Param("deptId") Long deptId, @Param("fecha") LocalDate fecha);
    
    // Verificar si existe presupuesto vigente para un departamento
    @Query("SELECT COUNT(p) > 0 FROM PresupuestoAreas p WHERE p.departamento.deptId = :deptId " +
           "AND p.presIniVigencia <= :fecha AND p.presFinVigencia >= :fecha")
    boolean existePresupuestoVigentePorDepartamento(@Param("deptId") Long deptId, 
                                                    @Param("fecha") LocalDate fecha);
    
    // Obtener todos ordenados por departamento
    List<PresupuestoAreas> findAllByOrderByDepartamento_DeptNomAsc();
}