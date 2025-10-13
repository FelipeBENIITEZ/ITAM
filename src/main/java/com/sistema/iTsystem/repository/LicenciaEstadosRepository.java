package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.LicenciasEstados;

@Repository
public interface LicenciaEstadosRepository extends JpaRepository<LicenciasEstados, Long> {
    
    // Buscar por nombre
    Optional<LicenciasEstados> findByLicEstadoNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsByLicEstadoNom(String nombre);
}