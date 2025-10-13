package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.LicenciasTipo;

@Repository
public interface LicenciaTipoRepository extends JpaRepository<LicenciasTipo, Long> {
    
    // Buscar por nombre
    Optional<LicenciasTipo> findByLicTipoNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsByLicTipoNom(String nombre);
}