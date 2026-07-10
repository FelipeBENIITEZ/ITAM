package com.sistema.iTsystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.SolicitudHistorialEstados;

@Repository
public interface SolicitudHistorialEstadosRepository extends JpaRepository<SolicitudHistorialEstados, Long> {

    @Query("SELECT h FROM SolicitudHistorialEstados h " +
           "LEFT JOIN FETCH h.estadoAnterior " +
           "LEFT JOIN FETCH h.estadoNuevo " +
           "LEFT JOIN FETCH h.usuario u " +
           "LEFT JOIN FETCH u.persona " +
           "WHERE h.solicitud.soliId = :solicitudId " +
           "ORDER BY h.fechaCambio DESC")
    List<SolicitudHistorialEstados> findBySolicitudIdWithDetails(@Param("solicitudId") Long solicitudId);
}
