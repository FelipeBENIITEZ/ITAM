package com.sistema.iTsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.CategoriasActivo;

@Repository
public interface CategoriasActivoRepository extends JpaRepository<CategoriasActivo, Long> {
    
    // Buscar categorías por nombre
    List<CategoriasActivo> findByCatNomContainingIgnoreCase(String nombre);
    
    // Buscar por nombre exacto
    CategoriasActivo findByCatNom(String nombre);
    
    // Verificar si existe una categoría por nombre
    boolean existsByCatNom(String nombre);
    
    // Obtener todas las categorías ordenadas por nombre
    List<CategoriasActivo> findAllByOrderByCatNomAsc();
}