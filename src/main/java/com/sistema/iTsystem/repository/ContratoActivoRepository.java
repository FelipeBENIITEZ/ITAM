package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.ContratoActivo;
import com.sistema.iTsystem.model.ContratoInfo;

@Repository
public interface ContratoActivoRepository extends JpaRepository<ContratoActivo, ContratoActivo.ContratoActivoId> {
    
    // Buscar contratos de un activo
    List<ContratoActivo> findByActivo(Activo activo);
    
    // Buscar contratos de un activo por ID
    List<ContratoActivo> findByActivo_ActivoId(Long activoId);
    
    // Buscar activos de un contrato
    List<ContratoActivo> findByContratoInfo(ContratoInfo contratoInfo);
    
    // Buscar activos de un contrato por ID
    List<ContratoActivo> findByContratoInfo_ContratId(Long contratId);
    
    // Verificar si existe vinculación
    boolean existsByActivo_ActivoIdAndContratoInfo_ContratId(Long activoId, Long contratId);
    
    // Buscar vinculación específica
    Optional<ContratoActivo> findByActivo_ActivoIdAndContratoInfo_ContratId(Long activoId, Long contratId);
    
    // Contar contratos de un activo
    @Query("SELECT COUNT(ca) FROM ContratoActivo ca WHERE ca.activo.activoId = :activoId")
    Long countContratosPorActivo(@Param("activoId") Long activoId);
    
    // Contar activos de un contrato
    @Query("SELECT COUNT(ca) FROM ContratoActivo ca WHERE ca.contratoInfo.contratId = :contratId")
    Long countActivosPorContrato(@Param("contratId") Long contratId);
    
    // Query optimizada con todas las relaciones
    @Query("SELECT ca FROM ContratoActivo ca " +
           "LEFT JOIN FETCH ca.activo a " +
           "LEFT JOIN FETCH ca.contratoInfo c " +
           "LEFT JOIN FETCH c.proveedor " +
           "WHERE ca.activo.activoId = :activoId")
    List<ContratoActivo> findByActivoIdWithDetails(@Param("activoId") Long activoId);
    
    // Obtener últimas vinculaciones
    @Query("SELECT ca FROM ContratoActivo ca " +
           "ORDER BY ca.fechaVinculacion DESC")
    List<ContratoActivo> findTop10ByOrderByFechaVinculacionDesc();
}
