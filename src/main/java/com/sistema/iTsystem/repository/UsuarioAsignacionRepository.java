package com.sistema.iTsystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.UsuarioAsignacion;

@Repository
public interface UsuarioAsignacionRepository extends JpaRepository<UsuarioAsignacion, Long> {

    List<UsuarioAsignacion> findByActivo_ActivoIdOrderByAsignacionFechaDesc(Long activoId);

    List<UsuarioAsignacion> findByUsuario_UsuId(Long usuarioId);

    Optional<UsuarioAsignacion> findByActivo_ActivoIdAndAsignacionActivaTrue(Long activoId);

    List<UsuarioAsignacion> findByAsignacionActivaTrueOrderByAsignacionFechaDesc();

    @Query("SELECT ua FROM UsuarioAsignacion ua " +
           "LEFT JOIN FETCH ua.usuario u " +
           "LEFT JOIN FETCH u.persona " +
           "LEFT JOIN FETCH u.departamento " +
           "WHERE ua.activo.activoId = :activoId " +
           "ORDER BY ua.asignacionActiva DESC, ua.asignacionFecha DESC")
    List<UsuarioAsignacion> findByActivoIdWithUserDetails(@Param("activoId") Long activoId);
}
