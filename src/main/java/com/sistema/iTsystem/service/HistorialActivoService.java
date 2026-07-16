package com.sistema.iTsystem.service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sistema.iTsystem.dto.activos.EventoHistorialActivoDTO;
import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.ActivoHistorialEstados;
import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.repository.ActivoHistorialEstadosRepository;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;

@Service
public class HistorialActivoService {

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private ActivoHistorialEstadosRepository historialEstadosRepository;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    public List<EventoHistorialActivoDTO> obtenerHistorialOperativo(Long activoId) {
        Activo activo = activoRepository.findById(activoId)
            .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

        List<EventoHistorialActivoDTO> eventos = new ArrayList<>();
        agregarEventosDeEstados(activo, eventos);
        agregarEventosDeAsignaciones(activo, eventos);

        return eventos.stream()
            .sorted(Comparator.comparing(EventoHistorialActivoDTO::getFecha,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(
                    (EventoHistorialActivoDTO evento) -> evento.getPrioridadOrden() != null ? evento.getPrioridadOrden() : 0,
                    Comparator.reverseOrder()
                ))
            .toList();
    }

    private void agregarEventosDeEstados(Activo activo, List<EventoHistorialActivoDTO> eventos) {
        List<ActivoHistorialEstados> historialEstados = historialEstadosRepository.findByActivoIdWithDetails(activo.getActivoId());
        for (ActivoHistorialEstados historial : historialEstados) {
            if (historial == null || historial.getEstadoNuevo() == null) {
                continue;
            }

            String estadoNuevo = historial.getEstadoNuevoNombre();
            String estadoAnterior = historial.getEstadoAnteriorNombre();
            EventoHistorialActivoDTO dto = new EventoHistorialActivoDTO();
            dto.setFecha(historial.getFechaCambio());
            dto.setEstadoAnterior(historial.getEstadoAnterior() != null ? estadoAnterior : null);
            dto.setEstadoNuevo(estadoNuevo);
            dto.setUsuarioEjecutor(historial.getUsuarioNombre());
            dto.setMotivo(historial.getMotivo());
            dto.setObservacion(historial.getObservaciones());
            dto.setPrioridadOrden(10);

            if (historial.esRegistroInicial()) {
                dto.setTipoEvento("ALTA");
                dto.setTitulo("Alta del activo");
                dto.setDescripcion("Ingreso inicial al inventario.");
                dto.setPrioridadOrden(90);
                eventos.add(dto);
                continue;
            }

            if ("Asignado".equalsIgnoreCase(estadoNuevo)) {
                continue;
            }

            if ("En mantenimiento".equalsIgnoreCase(estadoNuevo)) {
                dto.setTipoEvento("MANTENIMIENTO_INICIO");
                dto.setTitulo("Inicio de mantenimiento");
                dto.setDescripcion(construirDescripcionEstado(estadoAnterior, estadoNuevo));
                dto.setPrioridadOrden(80);
                eventos.add(dto);
                continue;
            }

            if ("Disponible".equalsIgnoreCase(estadoAnterior)
                    && "Disponible".equalsIgnoreCase(estadoNuevo)) {
                continue;
            }

            if ("Disponible".equalsIgnoreCase(estadoAnterior)
                    && "Dado de baja".equalsIgnoreCase(estadoNuevo)) {
                dto.setTipoEvento("BAJA");
                dto.setTitulo("Baja del activo");
                dto.setDescripcion(construirDescripcionEstado(estadoAnterior, estadoNuevo));
                dto.setPrioridadOrden(70);
                eventos.add(dto);
                continue;
            }

            if ("En mantenimiento".equalsIgnoreCase(estadoAnterior)
                    && "Disponible".equalsIgnoreCase(estadoNuevo)) {
                dto.setTipoEvento("MANTENIMIENTO_FIN");
                dto.setTitulo("Fin de mantenimiento");
                dto.setDescripcion(construirDescripcionEstado(estadoAnterior, estadoNuevo));
                dto.setPrioridadOrden(75);
                eventos.add(dto);
                continue;
            }

            if ("Dado de baja".equalsIgnoreCase(estadoNuevo)) {
                dto.setTipoEvento("BAJA");
                dto.setTitulo("Baja del activo");
                dto.setDescripcion(construirDescripcionEstado(estadoAnterior, estadoNuevo));
                dto.setPrioridadOrden(70);
                eventos.add(dto);
                continue;
            }

            dto.setTipoEvento("CAMBIO_ESTADO");
            dto.setTitulo("Cambio de estado");
            dto.setDescripcion(construirDescripcionEstado(estadoAnterior, estadoNuevo));
            dto.setPrioridadOrden(40);
            eventos.add(dto);
        }
    }

    private void agregarEventosDeAsignaciones(Activo activo, List<EventoHistorialActivoDTO> eventos) {
        List<UsuarioAsignacion> asignaciones = usuarioAsignacionRepository.findByActivoIdWithUserDetails(activo.getActivoId());
        Map<Long, List<UsuarioAsignacion>> reasignacionesPorSolicitud = new HashMap<>();
        Set<Long> idsOrigenReasignacion = new HashSet<>();
        Set<Long> idsNuevaReasignacion = new HashSet<>();

        for (UsuarioAsignacion asignacion : asignaciones) {
            if (asignacion == null || asignacion.getAsignacionId() == null) {
                continue;
            }

            Solicitudes solicitud = asignacion.getSolicitud();
            if (solicitud == null || solicitud.getSoliId() == null
                    || !esTipoSolicitud(solicitud.getTipoSolicitud(), "reasignacion")) {
                continue;
            }

            reasignacionesPorSolicitud.computeIfAbsent(solicitud.getSoliId(), key -> new ArrayList<>())
                .add(asignacion);
        }

        List<Map.Entry<Long, List<UsuarioAsignacion>>> grupos = new ArrayList<>(reasignacionesPorSolicitud.entrySet());
        grupos.sort(Comparator.comparing(
            entry -> fechaGrupoReasignacion(entry.getValue()),
            Comparator.nullsLast(Comparator.naturalOrder())
        ));

        for (Map.Entry<Long, List<UsuarioAsignacion>> entrada : grupos) {
            List<UsuarioAsignacion> grupo = entrada.getValue();
            if (grupo == null || grupo.size() < 2) {
                continue;
            }

            grupo.sort(Comparator.comparing(
                this::fechaEventoAsignacion,
                Comparator.nullsLast(Comparator.naturalOrder())
            ));

            UsuarioAsignacion anterior = grupo.stream()
                .filter(item -> !Boolean.TRUE.equals(item.getAsignacionActiva()))
                .min(Comparator.comparing(this::fechaEventoAsignacion, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
            UsuarioAsignacion nueva = grupo.stream()
                .filter(item -> Boolean.TRUE.equals(item.getAsignacionActiva()))
                .max(Comparator.comparing(this::fechaEventoAsignacion, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);

            if (anterior == null || nueva == null) {
                anterior = grupo.get(0);
                nueva = grupo.get(grupo.size() - 1);
                if (anterior.getAsignacionId().equals(nueva.getAsignacionId())) {
                    continue;
                }
            }

            eventos.add(construirEventoReasignacion(anterior, nueva));
            if (anterior.getAsignacionId() != null) {
                idsOrigenReasignacion.add(anterior.getAsignacionId());
            }
            if (nueva.getAsignacionId() != null) {
                idsNuevaReasignacion.add(nueva.getAsignacionId());
            }
        }

        for (UsuarioAsignacion asignacion : asignaciones) {
            if (asignacion == null || asignacion.getAsignacionId() == null) {
                continue;
            }

            Long asignacionId = asignacion.getAsignacionId();
            if (idsNuevaReasignacion.contains(asignacionId)) {
                continue;
            }

            eventos.add(construirEventoAsignacion(asignacion));

            if (!idsOrigenReasignacion.contains(asignacionId) && asignacion.getDevolucionFecha() != null) {
                eventos.add(construirEventoDevolucion(asignacion));
            }
        }
    }

    private EventoHistorialActivoDTO construirEventoAsignacion(UsuarioAsignacion asignacion) {
        EventoHistorialActivoDTO dto = new EventoHistorialActivoDTO();
        dto.setFecha(obtenerFechaEventoAsignacion(asignacion));
        dto.setTipoEvento("ASIGNACION");
        dto.setTitulo("Asignacion");
        dto.setDescripcion("Activo asignado a " + nombreUsuario(asignacion.getUsuario()));
        dto.setUsuarioAnterior(null);
        dto.setUsuarioNuevo(nombreUsuario(asignacion.getUsuario()));
        dto.setUsuarioEjecutor(nombreUsuario(asignacion.getUsuarioEjecutor()));
        dto.setMotivo(asignacion.getAsignacionMotivo());
        dto.setObservacion(asignacion.getAsignacionObservacion());
        dto.setSolicitudId(obtenerSolicitudId(asignacion));
        dto.setPrioridadOrden(60);
        return dto;
    }

    private EventoHistorialActivoDTO construirEventoReasignacion(UsuarioAsignacion anterior, UsuarioAsignacion nueva) {
        EventoHistorialActivoDTO dto = new EventoHistorialActivoDTO();
        dto.setFecha(obtenerFechaEventoAsignacion(nueva));
        dto.setTipoEvento("REASIGNACION");
        dto.setTitulo("Reasignacion");
        dto.setDescripcion(nombreUsuario(anterior.getUsuario()) + " -> " + nombreUsuario(nueva.getUsuario()));
        dto.setUsuarioAnterior(nombreUsuario(anterior.getUsuario()));
        dto.setUsuarioNuevo(nombreUsuario(nueva.getUsuario()));
        dto.setUsuarioEjecutor(nombreUsuario(nueva.getUsuarioEjecutor() != null ? nueva.getUsuarioEjecutor() : anterior.getUsuarioEjecutor()));
        dto.setMotivo(nueva.getAsignacionMotivo() != null ? nueva.getAsignacionMotivo() : anterior.getAsignacionMotivo());
        dto.setObservacion(nueva.getAsignacionObservacion() != null ? nueva.getAsignacionObservacion() : anterior.getAsignacionObservacion());
        dto.setSolicitudId(obtenerSolicitudId(nueva) != null ? obtenerSolicitudId(nueva) : obtenerSolicitudId(anterior));
        dto.setPrioridadOrden(100);
        return dto;
    }

    private EventoHistorialActivoDTO construirEventoDevolucion(UsuarioAsignacion asignacion) {
        EventoHistorialActivoDTO dto = new EventoHistorialActivoDTO();
        dto.setFecha(fechaEventoDevolucion(asignacion));
        dto.setTipoEvento("DEVOLUCION");
        dto.setTitulo("Devolucion");
        dto.setDescripcion("Activo devuelto por " + nombreUsuario(asignacion.getUsuario()));
        dto.setUsuarioAnterior(nombreUsuario(asignacion.getUsuario()));
        dto.setUsuarioEjecutor(nombreUsuario(asignacion.getUsuarioEjecutor()));
        dto.setMotivo(asignacion.getAsignacionMotivo());
        dto.setObservacion(asignacion.getAsignacionObservacion());
        dto.setSolicitudId(obtenerSolicitudId(asignacion));
        dto.setPrioridadOrden(70);
        return dto;
    }

    private String construirDescripcionEstado(String estadoAnterior, String estadoNuevo) {
        if (estadoAnterior == null || estadoAnterior.isBlank()) {
            return "Estado actualizado a " + estadoNuevo;
        }
        return "Estado cambiado de " + estadoAnterior + " a " + estadoNuevo;
    }

    private LocalDateTime obtenerFechaEventoAsignacion(UsuarioAsignacion asignacion) {
        if (asignacion.getCreatedAt() != null) {
            return asignacion.getCreatedAt();
        }
        LocalDate fecha = asignacion.getAsignacionFecha();
        return fecha != null ? fecha.atTime(LocalTime.NOON) : LocalDateTime.now();
    }

    private LocalDateTime fechaEventoAsignacion(UsuarioAsignacion asignacion) {
        return obtenerFechaEventoAsignacion(asignacion);
    }

    private LocalDateTime fechaGrupoReasignacion(List<UsuarioAsignacion> grupo) {
        return grupo == null ? null : grupo.stream()
            .map(this::fechaEventoAsignacion)
            .filter(fecha -> fecha != null)
            .min(Comparator.naturalOrder())
            .orElse(null);
    }

    private LocalDateTime fechaEventoDevolucion(UsuarioAsignacion asignacion) {
        if (asignacion.getUpdatedAt() != null) {
            return asignacion.getUpdatedAt();
        }
        if (asignacion.getDevolucionFecha() != null) {
            return asignacion.getDevolucionFecha().atTime(LocalTime.NOON);
        }
        return fechaEventoAsignacion(asignacion);
    }

    private Long obtenerSolicitudId(UsuarioAsignacion asignacion) {
        return asignacion != null && asignacion.getSolicitud() != null
            ? asignacion.getSolicitud().getSoliId()
            : null;
    }

    private String nombreUsuario(com.sistema.iTsystem.model.Usuario usuario) {
        if (usuario == null) {
            return "No registrado";
        }
        if (usuario.getPersona() != null) {
            String nombre = (usuario.getPersona().getPerNom1() != null ? usuario.getPersona().getPerNom1() : "").trim();
            String apellido = (usuario.getPersona().getPerApe1() != null ? usuario.getPersona().getPerApe1() : "").trim();
            String completo = (nombre + " " + apellido).trim();
            if (!completo.isEmpty()) {
                return completo;
            }
        }
        return usuario.getUsuLogin() != null ? usuario.getUsuLogin() : "No registrado";
    }

    private boolean esTipoSolicitud(String tipoSolicitud, String tipoEsperadoNormalizado) {
        return normalizarTexto(tipoSolicitud).equals(tipoEsperadoNormalizado);
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}+", "").trim().toLowerCase();
    }
}
