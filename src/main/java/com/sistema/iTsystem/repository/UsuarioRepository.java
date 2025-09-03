package com.sistema.iTsystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.Usuario;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    
    // Buscar por login
    Optional<Usuario> findByUsuLogin(String usuLogin);
    
    // Buscar por email
    Optional<Usuario> findByUsuMail(String usuMail);
    
    // Validar existencia por login
    boolean existsByUsuLogin(String usuLogin);
    
    // Validar existencia por email
    boolean existsByUsuMail(String usuMail);
}
