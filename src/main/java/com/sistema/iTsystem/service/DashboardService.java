package com.sistema.iTsystem.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.sistema.iTsystem.dto.dashboard.DashboardDTO;
import com.sistema.iTsystem.dto.dashboard.MetricaDashboardDTO;
import com.sistema.iTsystem.dto.dashboard.SolicitudRecienteDTO;
import com.sistema.iTsystem.model.CategoriasActivo;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.SoliEstados;
import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
import com.sistema.iTsystem.repository.SoliEstadosRepository;
import com.sistema.iTsystem.repository.SolicitudesRepository;

@Service
public class DashboardService {

    private static final List<String> ESTADOS_ACTIVO_ORDEN = List.of(
        "Disponible",
        "Asignado",
        "En mantenimiento",
        "Dado de baja"
    );
    private static final List<String> ESTADOS_SOLICITUD_ORDEN = List.of(
        "Pendiente",
        "En analisis",
        "Aprobada",
        "En ejecucion",
        "Resuelta",
        "Cerrada",
        "Rechazada"
    );
    private static final List<String> COLORES = List.of(
        "dashboard-accent-primary",
        "dashboard-accent-success",
        "dashboard-accent-warning",
        "dashboard-accent-info",
        "dashboard-accent-danger",
        "dashboard-accent-muted"
    );

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private SolicitudesRepository solicitudesRepository;

    @Autowired
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private CategoriasActivoRepository categoriasActivoRepository;

    @Autowired
    private SoliEstadosRepository soliEstadosRepository;

    public DashboardDTO obtenerDashboard() {
        DashboardDTO dashboard = new DashboardDTO();

        Map<String, Long> activosPorEstadoRaw = aMapa(activoRepository.countActivosPorEstado());
        Map<String, Long> activosPorCategoriaRaw = aMapa(activoRepository.countActivosPorCategoria());
        Map<String, Long> solicitudesPorEstadoRaw = normalizarEstadosSolicitudesVista(aMapa(solicitudesRepository.countSolicitudesPorEstado()));
        List<EstadoActivo> estadosActivos = estadoActivoRepository.findAll();
        List<SoliEstados> estadosSolicitudes = soliEstadosRepository.findAll();
        List<CategoriasActivo> categorias = categoriasActivoRepository.findAll();

        long totalActivos = activoRepository.count();
        long totalSolicitudes = solicitudesRepository.count();

        Long idDisponible = buscarEstadoActivoId("Disponible", estadosActivos);
        Long idAsignado = buscarEstadoActivoId("Asignado", estadosActivos);
        Long idMantenimiento = buscarEstadoActivoId("En mantenimiento", estadosActivos);
        Long idDadoDeBaja = buscarEstadoActivoId("Dado de baja", estadosActivos);
        Long idPendiente = buscarSoliEstadoId("Pendiente", estadosSolicitudes);
        Long idEnAnalisis = buscarSoliEstadoId("En analisis", estadosSolicitudes);
        Long idEnEjecucion = buscarSoliEstadoId("En ejecucion", estadosSolicitudes);
        Map<String, Long> categoriasIds = categorias.stream().collect(Collectors.toMap(
            categoria -> normalizarTextoSeguro(categoria.getCatNom()),
            CategoriasActivo::getCatId,
            (a, b) -> a,
            LinkedHashMap::new
        ));

        dashboard.setTotalActivos(totalActivos);
        dashboard.setTotalSolicitudes(totalSolicitudes);
        dashboard.setActivosDisponibles(valorDe(activosPorEstadoRaw, "Disponible"));
        dashboard.setActivosAsignados(valorDe(activosPorEstadoRaw, "Asignado"));
        dashboard.setActivosEnMantenimiento(valorDe(activosPorEstadoRaw, "En mantenimiento"));
        dashboard.setActivosDadosDeBaja(valorDe(activosPorEstadoRaw, "Dado de baja"));
        dashboard.setSolicitudesPendientes(valorDe(solicitudesPorEstadoRaw, "Pendiente"));
        dashboard.setSolicitudesEnAnalisis(valorDe(solicitudesPorEstadoRaw, "En analisis"));
        dashboard.setSolicitudesEnEjecucion(valorDe(solicitudesPorEstadoRaw, "En ejecucion"));

        dashboard.setTarjetas(List.of(
            crearKpi("total_activos", "Total de activos", totalActivos, "/activos", "icon-activos", "dashboard-accent-primary"),
            crearKpi("activos_disponibles", "Activos disponibles", dashboard.getActivosDisponibles(), urlActivosPorEstado(idDisponible), "icon-activos", "dashboard-accent-success"),
            crearKpi("activos_asignados", "Activos asignados", dashboard.getActivosAsignados(), urlActivosPorEstado(idAsignado), "icon-activos", "dashboard-accent-info"),
            crearKpi("solicitudes_pendientes", "Solicitudes pendientes", dashboard.getSolicitudesPendientes(), urlSolicitudesPorEstado(idPendiente), "icon-solicitudes", "dashboard-accent-danger")
        ));

        dashboard.setActivosPorEstado(construirMetricasPorEstado(activosPorEstadoRaw, ESTADOS_ACTIVO_ORDEN, totalActivos,
            estadosActivos.stream().collect(Collectors.toMap(
                estado -> normalizarTextoSeguro(estado.getEstadoNom()),
                estado -> estado.getEstadoId(),
                (a, b) -> a,
                LinkedHashMap::new
            )), this::urlActivosPorEstado, this::colorClaseEstadoActivo, false));

        dashboard.setActivosPorCategoria(construirCategorias(activosPorCategoriaRaw, totalActivos, categoriasIds));

        dashboard.setSolicitudesPorEstado(construirMetricasPorEstado(solicitudesPorEstadoRaw, ESTADOS_SOLICITUD_ORDEN, totalSolicitudes,
            estadosSolicitudes.stream().collect(Collectors.toMap(
                estado -> normalizarTextoSeguro(estado.getSoliEstadoNom()),
                estado -> estado.getSoliEstadoId(),
                (a, b) -> a,
                LinkedHashMap::new
            )), this::urlSolicitudesPorEstado, this::colorClaseEstadoSolicitud, false));

        dashboard.setSolicitudesAtencion(construirSolicitudesAtencion(dashboard.getSolicitudesPorEstado()));
        dashboard.setIndicadoresSecundarios(construirIndicadoresSecundarios(
            dashboard.getActivosEnMantenimiento(),
            dashboard.getActivosDadosDeBaja(),
            dashboard.getSolicitudesEnAnalisis(),
            dashboard.getSolicitudesEnEjecucion(),
            urlActivosPorEstado(idMantenimiento),
            urlActivosPorEstado(idDadoDeBaja),
            urlSolicitudesPorEstado(idEnAnalisis),
            urlSolicitudesPorEstado(idEnEjecucion)
        ));

        dashboard.setSolicitudesRecientes(construirSolicitudesRecientes());

        return dashboard;
    }

    private List<SolicitudRecienteDTO> construirSolicitudesRecientes() {
        List<Solicitudes> recientes = solicitudesRepository.findUltimasConDetalles(PageRequest.of(0, 5));
        if (recientes == null || recientes.isEmpty()) {
            return Collections.emptyList();
        }

        return recientes.stream()
            .map(solicitud -> {
                SolicitudRecienteDTO dto = new SolicitudRecienteDTO();
                dto.setId(solicitud.getSoliId());
                dto.setFecha(solicitud.getCreatedAt());
                dto.setTipo(solicitud.getTipoSolicitud());
                dto.setSolicitante(solicitud.getSolicitanteNombre());
                dto.setResponsable(solicitud.getResponsableNombre());
                dto.setEstado(normalizarEstadoSolicitudVista(solicitud.getEstadoSolicitud()));
                dto.setUrl("/solicitudes/" + solicitud.getSoliId());
                dto.setBadgeClase(claseBadgeEstadoSolicitud(dto.getEstado()));
                return dto;
            })
            .toList();
    }

    private List<MetricaDashboardDTO> construirSolicitudesAtencion(List<MetricaDashboardDTO> solicitudesPorEstado) {
        if (solicitudesPorEstado == null) {
            return List.of();
        }

        List<String> estadosAtencion = List.of(
            "Pendiente",
            "En analisis",
            "Aprobada",
            "En ejecucion"
        );

        return solicitudesPorEstado.stream()
            .filter(item -> item != null
                && item.getCantidad() != null
                && item.getCantidad() > 0
                && estadosAtencion.stream().anyMatch(estado -> normalizarTextoSeguro(estado).equals(normalizarTextoSeguro(item.getNombre()))))
            .toList();
    }

    private List<MetricaDashboardDTO> construirIndicadoresSecundarios(
            Long activosEnMantenimiento,
            Long activosDadosDeBaja,
            Long solicitudesEnAnalisis,
            Long solicitudesEnEjecucion,
            String urlActivosEnMantenimiento,
            String urlActivosDadosDeBaja,
            String urlSolicitudesEnAnalisis,
            String urlSolicitudesEnEjecucion) {

        List<MetricaDashboardDTO> indicadores = new ArrayList<>();
        indicadores.add(crearMetrica("sec_activos_mantenimiento", "Activos en mantenimiento", activosEnMantenimiento, 0.0, urlActivosEnMantenimiento, "dashboard-accent-warning"));
        indicadores.add(crearMetrica("sec_activos_baja", "Activos dados de baja", activosDadosDeBaja, 0.0, urlActivosDadosDeBaja, "dashboard-accent-danger"));
        indicadores.add(crearMetrica("sec_solicitudes_analisis", "Solicitudes en analisis", solicitudesEnAnalisis, 0.0, urlSolicitudesEnAnalisis, "dashboard-accent-warning"));
        indicadores.add(crearMetrica("sec_solicitudes_ejecucion", "Solicitudes en ejecucion", solicitudesEnEjecucion, 0.0, urlSolicitudesEnEjecucion, "dashboard-accent-info"));
        return indicadores;
    }

    private Map<String, Long> normalizarEstadosSolicitudesVista(Map<String, Long> valoresRaw) {
        Map<String, Long> resultado = new LinkedHashMap<>();
        if (valoresRaw == null) {
            return resultado;
        }

        for (Map.Entry<String, Long> entry : valoresRaw.entrySet()) {
            String estado = normalizarEstadoSolicitudVista(entry.getKey());
            resultado.merge(estado, entry.getValue() != null ? entry.getValue() : 0L, Long::sum);
        }

        return resultado;
    }

    private List<MetricaDashboardDTO> construirCategorias(Map<String, Long> categoriasRaw, long total, Map<String, Long> ids) {
        List<Map.Entry<String, Long>> ordenadas = categoriasRaw.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)))
            .toList();

        List<MetricaDashboardDTO> resultado = new ArrayList<>();
        long otros = 0L;

        for (Map.Entry<String, Long> entrada : ordenadas) {
            Long cantidad = entrada.getValue() != null ? entrada.getValue() : 0L;
            if (esCategoriaOtros(entrada.getKey())) {
                otros += cantidad;
                continue;
            }

            if (resultado.size() < 8) {
                Long categoriaId = ids.get(normalizarTextoSeguro(entrada.getKey()));
                resultado.add(crearMetrica(
                    "cat_" + resultado.size(),
                    entrada.getKey(),
                    cantidad,
                    porcentaje(cantidad, total),
                    categoriaId != null ? "/activos?categoria=" + categoriaId : null,
                    colorPorIndice(resultado.size())
                ));
            } else {
                otros += cantidad;
            }
        }

        if (otros > 0) {
            resultado.add(crearMetrica("cat_otros", "Otros", otros, porcentaje(otros, total), null, "dashboard-accent-muted"));
        }

        return resultado;
    }

    private List<MetricaDashboardDTO> construirMetricasPorEstado(
            Map<String, Long> valoresRaw,
            List<String> orden,
            long total,
            Map<String, Long> ids,
            Function<Long, String> urlBuilder,
            Function<String, String> colorBuilder,
            boolean incluirOtros) {

        List<MetricaDashboardDTO> resultado = new ArrayList<>();

        for (String estado : orden) {
            Long cantidad = valorDe(valoresRaw, estado);
            Long id = ids.get(normalizarTextoSeguro(estado));
            resultado.add(crearMetrica(
                estado,
                estado,
                cantidad,
                porcentaje(cantidad, total),
                urlBuilder.apply(id),
                colorBuilder.apply(estado)
            ));
        }

        List<Map.Entry<String, Long>> restantes = valoresRaw.entrySet().stream()
            .filter(entry -> orden.stream().noneMatch(estado -> normalizarTextoSeguro(estado).equals(normalizarTextoSeguro(entry.getKey()))))
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
            .toList();

        long otros = 0L;
        for (Map.Entry<String, Long> entry : restantes) {
            Long cantidad = entry.getValue();
            if (cantidad != null && cantidad > 0) {
                otros += cantidad;
            }
        }

        if (incluirOtros && otros > 0) {
            resultado.add(crearMetrica(
                "otros",
                "Otros",
                otros,
                porcentaje(otros, total),
                null,
                "dashboard-accent-muted"
            ));
        }

        return resultado;
    }

    private MetricaDashboardDTO crearKpi(String codigo, String nombre, Long cantidad, String url, String iconoClase, String colorClase) {
        MetricaDashboardDTO dto = new MetricaDashboardDTO();
        dto.setCodigo(codigo);
        dto.setNombre(nombre);
        dto.setCantidad(cantidad != null ? cantidad : 0L);
        dto.setPorcentaje(0.0);
        dto.setUrl(url);
        dto.setIconoClase(iconoClase);
        dto.setColorClase(colorClase);
        return dto;
    }

    private MetricaDashboardDTO crearMetrica(String codigo, String nombre, Long cantidad, Double porcentaje, String url, String colorClase) {
        MetricaDashboardDTO dto = new MetricaDashboardDTO();
        dto.setCodigo(codigo);
        dto.setNombre(nombre);
        dto.setCantidad(cantidad != null ? cantidad : 0L);
        dto.setPorcentaje(porcentaje != null ? porcentaje : 0.0);
        dto.setUrl(url);
        dto.setColorClase(colorClase);
        return dto;
    }

    private Map<String, Long> aMapa(List<Object[]> filas) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        if (filas == null) {
            return mapa;
        }

        for (Object[] fila : filas) {
            if (fila == null || fila.length < 2 || fila[0] == null) {
                continue;
            }
            mapa.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }
        return mapa;
    }

    private Long valorDe(Map<String, Long> mapa, String clave) {
        if (mapa == null || clave == null) {
            return 0L;
        }
        String buscada = normalizarTextoSeguro(clave);
        return mapa.entrySet().stream()
            .filter(entry -> normalizarTextoSeguro(entry.getKey()).equals(buscada))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(0L);
    }

    private String urlActivosPorEstado(Long estadoId) {
        if (estadoId == null) {
            return "/activos";
        }
        return "/activos?estado=" + estadoId;
    }

    private String urlSolicitudesPorEstado(Long estadoId) {
        if (estadoId == null) {
            return "/solicitudes";
        }
        return "/solicitudes?estadoId=" + estadoId;
    }

    private String claseBadgeEstadoSolicitud(String estado) {
        String normalizado = normalizarTextoSeguro(estado);
        return switch (normalizado) {
            case "pendiente", "en analisis" -> "text-bg-warning";
            case "aprobada", "resuelta" -> "text-bg-success";
            case "en ejecucion" -> "text-bg-primary";
            case "cerrada", "completada" -> "text-bg-secondary";
            case "rechazada" -> "text-bg-danger";
            default -> "text-bg-secondary";
        };
    }

    private double porcentaje(long cantidad, long total) {
        if (total <= 0L || cantidad <= 0L) {
            return 0.0;
        }
        return Math.round((cantidad * 1000.0) / total) / 10.0;
    }

    private String colorPorIndice(int indice) {
        return COLORES.get(Math.min(indice, COLORES.size() - 1));
    }

    private String colorClaseEstadoActivo(String estado) {
        String normalizado = normalizarTextoSeguro(estado);
        return switch (normalizado) {
            case "disponible" -> "dashboard-accent-success";
            case "asignado" -> "dashboard-accent-info";
            case "en mantenimiento" -> "dashboard-accent-warning";
            case "dado de baja" -> "dashboard-accent-danger";
            default -> "dashboard-accent-muted";
        };
    }

    private String colorClaseEstadoSolicitud(String estado) {
        String normalizado = normalizarTextoSeguro(estado);
        return switch (normalizado) {
            case "pendiente", "en analisis" -> "dashboard-accent-warning";
            case "aprobada", "resuelta" -> "dashboard-accent-success";
            case "en ejecucion" -> "dashboard-accent-info";
            case "cerrada" -> "dashboard-accent-muted";
            case "rechazada" -> "dashboard-accent-danger";
            default -> "dashboard-accent-muted";
        };
    }

    private Long buscarEstadoActivoId(String nombre, List<EstadoActivo> estadosActivos) {
        String buscado = normalizarTextoSeguro(nombre);
        if (estadosActivos == null || estadosActivos.isEmpty()) {
            return null;
        }
        return estadosActivos.stream()
            .filter(estado -> normalizarTextoSeguro(estado.getEstadoNom()).equals(buscado))
            .map(EstadoActivo::getEstadoId)
            .findFirst()
            .orElse(null);
    }

    private Long buscarSoliEstadoId(String nombre, List<SoliEstados> estadosSolicitudes) {
        String buscado = normalizarTextoSeguro(nombre);
        if (estadosSolicitudes == null || estadosSolicitudes.isEmpty()) {
            return null;
        }
        return estadosSolicitudes.stream()
            .filter(estado -> normalizarTextoSeguro(estado.getSoliEstadoNom()).equals(buscado))
            .map(SoliEstados::getSoliEstadoId)
            .findFirst()
            .orElse(null);
    }

    private boolean esCategoriaOtros(String valor) {
        return "otros".equals(normalizarTextoSeguro(valor));
    }

    private String normalizarTextoSeguro(String valor) {
        if (valor == null) {
            return "";
        }

        return Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "")
            .trim()
            .toLowerCase(Locale.ROOT);
    }

    private String normalizarEstadoSolicitudVista(String estado) {
        String normalizado = normalizarTextoSeguro(estado);
        if (normalizado.equals("completada")) {
            return "Cerrada";
        }
        if (normalizado.equals("en analisis")) {
            return "En analisis";
        }
        if (normalizado.equals("en ejecucion")) {
            return "En ejecucion";
        }
        if (normalizado.equals("rechazada")) {
            return "Rechazada";
        }
        if (normalizado.equals("aprobada")) {
            return "Aprobada";
        }
        if (normalizado.equals("resuelta")) {
            return "Resuelta";
        }
        if (normalizado.equals("pendiente")) {
            return "Pendiente";
        }
        if (normalizado.equals("cerrada")) {
            return "Cerrada";
        }
        return estado != null ? estado.trim() : "";
    }
}
