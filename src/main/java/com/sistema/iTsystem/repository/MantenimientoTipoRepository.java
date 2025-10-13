package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.MantenimientoTipo;

@Repository
public interface MantenimientoTipoRepository extends JpaRepository<MantenimientoTipo, Long> {
    
    // Buscar por nombre
    Optional<MantenimientoTipo> findByMantTipoNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsByMantTipoNom(String nombre);
    
    // Buscar tipos que contengan texto
    java.util.List<MantenimientoTipo> findByMantTipoNomContainingIgnoreCase(String texto);
}