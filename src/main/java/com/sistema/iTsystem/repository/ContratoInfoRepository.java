package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.ContratoInfo;

@Repository
public interface ContratoInfoRepository extends JpaRepository<ContratoInfo, Long> {
    
    // Buscar por número de contrato
    Optional<ContratoInfo> findByContratNumero(String contratNumero);
    
    // Buscar contratos por proveedor
    @Query("SELECT c FROM ContratoInfo c WHERE c.proveedor.provId = :proveedorId ORDER BY c.createdAt DESC")
    List<ContratoInfo> findByProveedorId(@Param("proveedorId") Long proveedorId);
    
    // Buscar contratos que contengan cierto texto en la descripción
    @Query("SELECT c FROM ContratoInfo c WHERE LOWER(c.contratDescripcion) LIKE LOWER(CONCAT('%', :texto, '%')) ORDER BY c.createdAt DESC")
    List<ContratoInfo> buscarPorDescripcion(@Param("texto") String texto);
    
    // Obtener todos con información del proveedor (JOIN FETCH)
    @Query("SELECT c FROM ContratoInfo c JOIN FETCH c.proveedor ORDER BY c.createdAt DESC")
    List<ContratoInfo> findAllConProveedor();
    
    // Obtener todos ordenados por fecha de creación descendente
    List<ContratoInfo> findAllByOrderByCreatedAtDesc();
    
    // Obtener todos ordenados por número
    List<ContratoInfo> findAllByOrderByContratNumeroAsc();
    
    // Verificar si existe un contrato por número
    boolean existsByContratNumero(String contratNumero);
    
    // Contar contratos con archivo
    @Query("SELECT COUNT(c) FROM ContratoInfo c WHERE c.contratArchivoPath IS NOT NULL")
    Long countContratosConArchivo();
    
    // Obtener contratos sin archivo
    @Query("SELECT c FROM ContratoInfo c WHERE c.contratArchivoPath IS NULL ORDER BY c.createdAt DESC")
    List<ContratoInfo> findContratosSinArchivo();
    
    // Contar contratos por proveedor
    @Query("SELECT COUNT(c) FROM ContratoInfo c WHERE c.proveedor.provId = :proveedorId")
    Long countByProveedorId(@Param("proveedorId") Long proveedorId);
}