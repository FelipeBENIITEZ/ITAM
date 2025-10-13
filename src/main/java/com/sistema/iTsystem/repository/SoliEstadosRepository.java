package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.SoliEstados;

@Repository
public interface SoliEstadosRepository extends JpaRepository<SoliEstados, Long> {
    
    // Buscar por nombre
    Optional<SoliEstados> findBySoliEstadoNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsBySoliEstadoNom(String nombre);
    
    // Buscar estados que contengan texto
    java.util.List<SoliEstados> findBySoliEstadoNomContainingIgnoreCase(String texto);
}