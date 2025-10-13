package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Proveedores;

@Repository
public interface ProveedoresRepository extends JpaRepository<Proveedores, Long> {
    
    // Buscar por nombre
    Optional<Proveedores> findByProvNom(String nombre);
    
    // Verificar si existe por nombre
    boolean existsByProvNom(String nombre);
    
    // Buscar por NIT
    Optional<Proveedores> findByProvNit(String nit);
    
    // Verificar si existe por NIT
    boolean existsByProvNit(String nit);
}