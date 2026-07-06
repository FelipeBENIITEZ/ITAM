package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.CategoriasActivo;
import com.sistema.iTsystem.model.EstadoActivo;

@Repository
public interface ActivoRepository extends JpaRepository<Activo, Long> {

    Page<Activo> findByActivoNomContainingIgnoreCase(String nombre, Pageable pageable);

    Page<Activo> findByCategoria_CatId(Long categoriaId, Pageable pageable);

    Page<Activo> findByEstado_EstadoId(Long estadoId, Pageable pageable);

    Optional<Activo> findByActivoCodigo(String activoCodigo);

    Optional<Activo> findByActivoCodigoIgnoreCase(String activoCodigo);

    boolean existsByActivoCodigo(String activoCodigo);

    @Query("SELECT DISTINCT a FROM Activo a " +
           "LEFT JOIN UsuarioAsignacion ua ON ua.activo = a AND ua.asignacionActiva = true " +
           "LEFT JOIN ua.usuario u " +
           "WHERE (:texto IS NULL OR :texto = '' OR " +
           "LOWER(a.activoNom) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(a.activoCodigo) LIKE LOWER(CONCAT('%', :texto, '%'))) AND " +
           "(:categoriaId IS NULL OR a.categoria.catId = :categoriaId) AND " +
           "(:estadoId IS NULL OR a.estado.estadoId = :estadoId) AND " +
           "(:departamentoId IS NULL OR u.departamento.deptId = :departamentoId)")
    Page<Activo> findByFiltros(@Param("texto") String texto,
                               @Param("categoriaId") Long categoriaId,
                               @Param("estadoId") Long estadoId,
                               @Param("departamentoId") Long departamentoId,
                               Pageable pageable);

    List<Activo> findByEstado(EstadoActivo estado);

    List<Activo> findByCategoria(CategoriasActivo categoria);

    long countByEstado(EstadoActivo estado);

    long countByCategoria(CategoriasActivo categoria);

    @Query("SELECT COUNT(DISTINCT a) FROM Activo a " +
           "JOIN UsuarioAsignacion ua ON ua.activo = a AND ua.asignacionActiva = true " +
           "JOIN ua.usuario u " +
           "WHERE u.departamento.deptId = :departamentoId")
    long countByDepartamentoId(@Param("departamentoId") Long departamentoId);

    @Query("SELECT a.estado.estadoNom, COUNT(a) FROM Activo a GROUP BY a.estado.estadoNom")
    List<Object[]> countActivosPorEstado();

    @Query("SELECT a.categoria.catNom, COUNT(a) FROM Activo a GROUP BY a.categoria.catNom")
    List<Object[]> countActivosPorCategoria();

    @Query("SELECT u.departamento.deptNom, COUNT(DISTINCT a) FROM Activo a " +
           "JOIN UsuarioAsignacion ua ON ua.activo = a AND ua.asignacionActiva = true " +
           "JOIN ua.usuario u " +
           "GROUP BY u.departamento.deptNom")
    List<Object[]> countActivosPorDepartamento();
}
