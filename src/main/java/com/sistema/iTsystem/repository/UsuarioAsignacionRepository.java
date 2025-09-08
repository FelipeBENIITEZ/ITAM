package com.sistema.iTsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.model.UsuarioAsignacionId;

@Repository
public interface UsuarioAsignacionRepository extends JpaRepository<UsuarioAsignacion, UsuarioAsignacionId>{
    
    // Buscar asignaciones por activo ID
    List<UsuarioAsignacion> findByActivo_ActivoId(Long activoId);
    
    // Buscar asignaciones por usuario ID
    List<UsuarioAsignacion> findByUsuario_UsuId(Long usuarioId);
    
    // Query para obtener asignaciones con detalles de usuario y persona
    @Query("SELECT ua FROM UsuarioAsignacion ua " +
           "LEFT JOIN FETCH ua.usuario u " +
           "LEFT JOIN FETCH u.persona p " +
           "LEFT JOIN FETCH u.departamento d " +
           "WHERE ua.activo.activoId = :activoId " +
           "ORDER BY ua.asignacionFecha DESC")
    List<UsuarioAsignacion> findByActivoIdWithUserDetails(@Param("activoId") Long activoId);
}