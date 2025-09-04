package com.sistema.iTsystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Activo;

@Repository
public interface ActivoRepository extends JpaRepository<Activo, Long> {
    
    // Buscar activos por nombre (contiene texto)
    Page<Activo> findByActivoNomContainingIgnoreCase(String nombre, Pageable pageable);
    
    // Buscar por categoría
    Page<Activo> findByCategoria_CatId(Long categoriaId, Pageable pageable);
    
    // Buscar por estado
    Page<Activo> findByEstado_EstadoId(Long estadoId, Pageable pageable);
    
    
    // Query personalizada para filtros combinados
    @Query("SELECT a FROM Activo a WHERE " +
           "(:nombre IS NULL OR LOWER(a.activoNom) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:categoriaId IS NULL OR a.categoria.catId = :categoriaId) AND " +
           "(:estadoId IS NULL OR a.estado.estadoId = :estadoId)")
    Page<Activo> findByFiltros(@Param("nombre") String nombre,
                              @Param("categoriaId") Long categoriaId,
                              @Param("estadoId") Long estadoId,
                              Pageable pageable);
}