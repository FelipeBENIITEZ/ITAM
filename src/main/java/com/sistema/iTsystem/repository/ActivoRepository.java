package com.sistema.iTsystem.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.CategoriasActivo;
import com.sistema.iTsystem.model.Departamentos;
import com.sistema.iTsystem.model.EstadoActivo;

@Repository
public interface ActivoRepository extends JpaRepository<Activo, Long> {
    
    // ==================== BÚSQUEDAS CON PAGINACIÓN ====================
    
    // Buscar activos por nombre (contiene texto)
    Page<Activo> findByActivoNomContainingIgnoreCase(String nombre, Pageable pageable);
    
    // Buscar por categoría
    Page<Activo> findByCategoria_CatId(Long categoriaId, Pageable pageable);
    
    // Buscar por estado
    Page<Activo> findByEstado_EstadoId(Long estadoId, Pageable pageable);
    
    // Buscar por departamento (con paginación)
    Page<Activo> findByDepartamento_DeptId(Long deptId, Pageable pageable);
    
    // Query personalizada para filtros combinados
    @Query("SELECT a FROM Activo a WHERE " +
           "(:nombre IS NULL OR LOWER(a.activoNom) LIKE LOWER(CONCAT('%', :nombre, '%'))) AND " +
           "(:categoriaId IS NULL OR a.categoria.catId = :categoriaId) AND " +
           "(:estadoId IS NULL OR a.estado.estadoId = :estadoId) AND " +
           "(:deptId IS NULL OR a.departamento.deptId = :deptId)")
    Page<Activo> findByFiltros(@Param("nombre") String nombre,
                              @Param("categoriaId") Long categoriaId,
                              @Param("estadoId") Long estadoId,
                              @Param("deptId") Long deptId,
                              Pageable pageable);
    
    // ==================== BÚSQUEDAS SIN PAGINACIÓN ====================
    
    // Buscar por departamento
    List<Activo> findByDepartamento(Departamentos departamento);
    
    // Buscar por estado
    List<Activo> findByEstado(EstadoActivo estado);
    
    // Buscar por categoría
    List<Activo> findByCategoria(CategoriasActivo categoria);
    
    // ==================== CONTADORES ====================
    
    // Contar activos por estado
    long countByEstado(EstadoActivo estado);
    
    // Contar activos por categoría
    long countByCategoria(CategoriasActivo categoria);
    
    // Contar activos por departamento
    long countByDepartamento(Departamentos departamento);
    
    // ==================== ESTADÍSTICAS ====================
    
    // Obtener conteo de activos por estado (para dashboard)
    @Query("SELECT a.estado.estadoNom, COUNT(a) FROM Activo a GROUP BY a.estado.estadoNom")
    List<Object[]> countActivosPorEstado();
    
    // Obtener conteo de activos por categoría (para dashboard)
    @Query("SELECT a.categoria.catNom, COUNT(a) FROM Activo a GROUP BY a.categoria.catNom")
    List<Object[]> countActivosPorCategoria();
    
    // Obtener conteo de activos por departamento (para dashboard)
    @Query("SELECT a.departamento.deptNom, COUNT(a) FROM Activo a GROUP BY a.departamento.deptNom")
    List<Object[]> countActivosPorDepartamento();
}