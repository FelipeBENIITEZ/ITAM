package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.EventosNiveles;

@Repository
public interface EventosNivelesRepository extends JpaRepository<EventosNiveles, Long> {
    
    // Buscar por nombre
    Optional<EventosNiveles> findByNivelNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsByNivelNom(String nombre);
    
    // Buscar niveles que contengan texto
    java.util.List<EventosNiveles> findByNivelNomContainingIgnoreCase(String texto);
}
