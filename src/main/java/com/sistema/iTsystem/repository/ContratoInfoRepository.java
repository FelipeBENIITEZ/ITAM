package com.sistema.iTsystem.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.ContratoInfo;

@Repository
public interface ContratoInfoRepository extends JpaRepository<ContratoInfo, Long> {
    
    // Buscar contratos por proveedor
    List<ContratoInfo> findByProveedor_ProvId(Long provId);
    
    // Buscar contratos vigentes (que no hayan vencido)
    @Query("SELECT c FROM ContratoInfo c WHERE " +
           "(c.contratFechaFin IS NULL OR c.contratFechaFin >= :fecha) " +
           "ORDER BY c.contratNom")
    List<ContratoInfo> findContratosVigentes(@Param("fecha") LocalDate fecha);
    
    // Buscar contratos con información del proveedor
    @Query("SELECT c FROM ContratoInfo c " +
           "LEFT JOIN FETCH c.proveedor p " +
           "WHERE (c.contratFechaFin IS NULL OR c.contratFechaFin >= :fecha) " +
           "ORDER BY c.contratNom")
    List<ContratoInfo> findContratosVigentesConProveedor(@Param("fecha") LocalDate fecha);
    
    // Buscar contratos por nombre
    List<ContratoInfo> findByContratNomContainingIgnoreCase(String nombre);
    
    // Buscar contratos que vencen pronto (útil para alertas)
    @Query("SELECT c FROM ContratoInfo c WHERE " +
           "c.contratFechaFin BETWEEN :fechaInicio AND :fechaFin " +
           "ORDER BY c.contratFechaFin")
    List<ContratoInfo> findContratosQueVencen(@Param("fechaInicio") LocalDate fechaInicio, 
                                              @Param("fechaFin") LocalDate fechaFin);
    
    // Verificar si existe contrato vigente
    @Query("SELECT COUNT(c) > 0 FROM ContratoInfo c WHERE c.contratId = :contratoId " +
           "AND (c.contratFechaFin IS NULL OR c.contratFechaFin >= :fecha)")
    boolean existeContratoVigente(@Param("contratoId") Long contratoId, 
                                  @Param("fecha") LocalDate fecha);
    
    // Obtener todos ordenados por nombre
    List<ContratoInfo> findAllByOrderByContratNomAsc();
}