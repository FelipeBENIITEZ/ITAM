package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.SoftTipo;

@Repository
public interface SoftTipoRepository extends JpaRepository<SoftTipo, Long> {
    
    // Buscar por nombre
    Optional<SoftTipo> findBySoftTipoNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsBySoftTipoNom(String nombre);
}