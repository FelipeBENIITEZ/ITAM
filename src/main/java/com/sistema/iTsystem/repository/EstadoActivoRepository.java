package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.EstadoActivo;

@Repository
public interface EstadoActivoRepository extends JpaRepository<EstadoActivo, Long> {
    
    // Buscar estado por nombre exacto
    Optional<EstadoActivo> findByEstadoNom(String estadoNom);
    
    // Buscar estados por nombre (contiene texto)
    List<EstadoActivo> findByEstadoNomContainingIgnoreCase(String estadoNom);
    
    // Verificar si existe un estado por nombre
    boolean existsByEstadoNom(String estadoNom);
    
    // Obtener todos los estados ordenados por nombre
    List<EstadoActivo> findAllByOrderByEstadoNomAsc();
}