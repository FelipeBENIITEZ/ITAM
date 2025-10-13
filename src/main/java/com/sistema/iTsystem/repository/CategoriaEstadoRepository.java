package com.sistema.iTsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.CategoriaEstado;
import com.sistema.iTsystem.model.EstadoActivo;

@Repository
public interface CategoriaEstadoRepository extends JpaRepository<CategoriaEstado, Long> {

    /**
     * Obtener todos los estados permitidos para una categoría
     */
    @Query("SELECT ce.estado FROM CategoriaEstado ce WHERE ce.categoria.catId = :categoriaId ORDER BY ce.estado.estadoNom")
    List<EstadoActivo> findEstadosByCategoriaId(@Param("categoriaId") Long categoriaId);

    /**
     * Verificar si un estado es válido para una categoría
     */
    @Query("SELECT COUNT(ce) > 0 FROM CategoriaEstado ce " +
           "WHERE ce.categoria.catId = :categoriaId AND ce.estado.estadoId = :estadoId")
    boolean existsByCategoriaIdAndEstadoId(
        @Param("categoriaId") Long categoriaId, 
        @Param("estadoId") Long estadoId
    );

    /**
     * Obtener relaciones con detalles
     */
    @Query("SELECT ce FROM CategoriaEstado ce " +
           "JOIN FETCH ce.estado " +
           "JOIN FETCH ce.categoria " +
           "WHERE ce.categoria.catId = :categoriaId " +
           "ORDER BY ce.estado.estadoNom")
    List<CategoriaEstado> findByCategoriaIdWithDetails(@Param("categoriaId") Long categoriaId);
}