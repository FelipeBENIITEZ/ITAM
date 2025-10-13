package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.SoliTipos;

@Repository
public interface SoliTiposRepository extends JpaRepository<SoliTipos, Long> {
    
    // Buscar por nombre
    Optional<SoliTipos> findBySoliTipoNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsBySoliTipoNom(String nombre);
    
    // Buscar tipos que contengan texto
    java.util.List<SoliTipos> findBySoliTipoNomContainingIgnoreCase(String texto);
}
