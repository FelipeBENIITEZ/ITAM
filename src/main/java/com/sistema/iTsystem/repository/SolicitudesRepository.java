package com.sistema.iTsystem.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.sistema.iTsystem.model.SoliEstados;
import com.sistema.iTsystem.model.SoliTipos;
import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.model.Usuario;

@Repository
public interface SolicitudesRepository extends JpaRepository<Solicitudes, Long> {
    
    // ==================== BÚSQUEDAS BÁSICAS ====================
    
    // Buscar por usuario
    List<Solicitudes> findByUsuario(Usuario usuario);
    
    // Buscar por usuario ID
    List<Solicitudes> findByUsuario_UsuId(Long usuarioId);
    
    // Buscar por estado
    List<Solicitudes> findBySoliEstado(SoliEstados estado);
    
    // Buscar por estado ID
    List<Solicitudes> findBySoliEstado_SoliEstadoId(Long estadoId);
    
    // Buscar por tipo
    List<Solicitudes> findBySoliTipo(SoliTipos tipo);
    
    // Buscar por tipo ID
    List<Solicitudes> findBySoliTipo_SoliTipoId(Long tipoId);
    
    // ==================== BÚSQUEDAS CON PAGINACIÓN ====================
    
    // Buscar por usuario con paginación
    Page<Solicitudes> findByUsuario_UsuId(Long usuarioId, Pageable pageable);
    
    // Buscar por estado con paginación
    Page<Solicitudes> findBySoliEstado_SoliEstadoId(Long estadoId, Pageable pageable);
    
    // Buscar por tipo con paginación
    Page<Solicitudes> findBySoliTipo_SoliTipoId(Long tipoId, Pageable pageable);
    
    // ==================== BÚSQUEDAS AVANZADAS ====================
    
    // Buscar solicitudes pendientes
    @Query("SELECT s FROM Solicitudes s WHERE s.soliEstado.soliEstadoNom = 'Pendiente' ORDER BY s.createdAt DESC")
    List<Solicitudes> findSolicitudesPendientes();
    
    // Buscar solicitudes aprobadas
    @Query("SELECT s FROM Solicitudes s WHERE s.soliEstado.soliEstadoNom = 'Aprobada' ORDER BY s.createdAt DESC")
    List<Solicitudes> findSolicitudesAprobadas();
    
    // Buscar solicitudes rechazadas
    @Query("SELECT s FROM Solicitudes s WHERE s.soliEstado.soliEstadoNom = 'Rechazada' ORDER BY s.createdAt DESC")
    List<Solicitudes> findSolicitudesRechazadas();
    
    // Buscar por rango de fechas
    @Query("SELECT s FROM Solicitudes s WHERE s.createdAt BETWEEN :fechaInicio AND :fechaFin ORDER BY s.createdAt DESC")
    List<Solicitudes> findByFechaBetween(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                         @Param("fechaFin") LocalDateTime fechaFin);
    
    // Buscar por usuario y estado
    List<Solicitudes> findByUsuario_UsuIdAndSoliEstado_SoliEstadoId(Long usuarioId, Long estadoId);
    
    // Buscar por descripción
    @Query("SELECT s FROM Solicitudes s WHERE LOWER(s.soliDescri) LIKE LOWER(CONCAT('%', :texto, '%')) ORDER BY s.createdAt DESC")
    List<Solicitudes> findBySoliDescriContaining(@Param("texto") String texto);
    
    // ==================== QUERIES OPTIMIZADAS ====================
    
    // Query optimizada con todas las relaciones
    @Query("SELECT s FROM Solicitudes s " +
           "LEFT JOIN FETCH s.usuario u " +
           "LEFT JOIN FETCH u.persona " +
           "LEFT JOIN FETCH s.soliEstado " +
           "LEFT JOIN FETCH s.soliTipo " +
           "WHERE s.soliId = :soliId")
    Solicitudes findByIdWithDetails(@Param("soliId") Long soliId);
    
    // Obtener solicitudes con relaciones (paginado)
    @Query("SELECT s FROM Solicitudes s " +
           "LEFT JOIN FETCH s.usuario u " +
           "LEFT JOIN FETCH u.persona " +
           "LEFT JOIN FETCH s.soliEstado " +
           "LEFT JOIN FETCH s.soliTipo")
    List<Solicitudes> findAllWithDetails();
    
    // ==================== ESTADÍSTICAS ====================
    
    // Contar solicitudes por estado
    @Query("SELECT s.soliEstado.soliEstadoNom, COUNT(s) FROM Solicitudes s " +
           "GROUP BY s.soliEstado.soliEstadoNom")
    List<Object[]> countSolicitudesPorEstado();
    
    // Contar solicitudes por tipo
    @Query("SELECT s.soliTipo.soliTipoNom, COUNT(s) FROM Solicitudes s " +
           "GROUP BY s.soliTipo.soliTipoNom")
    List<Object[]> countSolicitudesPorTipo();
    
    // Contar solicitudes por usuario
    @Query("SELECT u.usuLogin, COUNT(s) FROM Solicitudes s " +
           "JOIN s.usuario u " +
           "GROUP BY u.usuLogin " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> countSolicitudesPorUsuario();
    
    // Contar solicitudes pendientes
    @Query("SELECT COUNT(s) FROM Solicitudes s WHERE s.soliEstado.soliEstadoNom = 'Pendiente'")
    Long countSolicitudesPendientes();
    
    // Obtener últimas solicitudes
    List<Solicitudes> findTop10ByOrderByCreatedAtDesc();
    
    // Obtener solicitudes de un usuario ordenadas
    List<Solicitudes> findByUsuario_UsuIdOrderByCreatedAtDesc(Long usuarioId);
}