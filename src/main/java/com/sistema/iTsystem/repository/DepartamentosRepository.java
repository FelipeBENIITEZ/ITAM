package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Departamentos;

@Repository
public interface DepartamentosRepository extends JpaRepository<Departamentos, Long> {
    
    // Buscar por nombre
    Optional<Departamentos> findByDeptNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsByDeptNom(String nombre);
}