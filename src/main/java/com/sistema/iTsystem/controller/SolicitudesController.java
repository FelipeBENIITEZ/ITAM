package com.sistema.iTsystem.controller;

import java.security.Principal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sistema.iTsystem.dto.solicitudes.SolicitudCambioEstadoDTO;
import com.sistema.iTsystem.dto.solicitudes.SolicitudFormDTO;
import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.SolicitudesService;

@Controller
@RequestMapping("/solicitudes")
public class SolicitudesController {

    @Autowired
    private SolicitudesService solicitudesService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String listar(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) Long solicitanteId,
            @RequestParam(required = false) Long responsableId,
            @RequestParam(required = false) LocalDate fechaDesde,
            @RequestParam(required = false) LocalDate fechaHasta,
            Principal principal,
            Model model) {

        Usuario usuarioActual = obtenerUsuarioActual(principal);
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), 10);
        Page<Solicitudes> paginaSolicitudes = solicitudesService.buscarConFiltros(
            texto,
            tipoId,
            estadoId,
            solicitanteId,
            responsableId,
            fechaDesde,
            fechaHasta,
            usuarioActual,
            pageable
        );

        cargarFiltrosListado(model, texto, tipoId, estadoId, solicitanteId, responsableId, fechaDesde, fechaHasta);
        model.addAttribute("solicitudes", paginaSolicitudes.getContent());
        model.addAttribute("paginaActual", paginaSolicitudes.getNumber());
        model.addAttribute("totalPaginas", paginaSolicitudes.getTotalPages());
        model.addAttribute("totalElementos", paginaSolicitudes.getTotalElements());
        model.addAttribute("usaLista", false);
        model.addAttribute("esSuperAdmin", solicitudesService.esSuperAdmin(usuarioActual));
        model.addAttribute("esTecnico", solicitudesService.esTecnico(usuarioActual));
        model.addAttribute("esStandardUser", solicitudesService.esStandardUser(usuarioActual));
        model.addAttribute("pendientes", solicitudesService.contarPendientes());

        return "solicitudes/listar";
    }

    @GetMapping("/nueva")
    public String nuevaForm(Principal principal, Model model) {
        Usuario usuarioActual = obtenerUsuarioActual(principal);
        cargarFormulario(model, new SolicitudFormDTO(), usuarioActual);
        return "solicitudes/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("solicitud") SolicitudFormDTO form,
                          Principal principal,
                          Model model,
                          RedirectAttributes flash) {
        Usuario usuarioActual = obtenerUsuarioActual(principal);
        try {
            Solicitudes guardada = solicitudesService.crearDesdeFormulario(form, usuarioActual);
            flash.addFlashAttribute("success", "Solicitud creada correctamente.");
            return "redirect:/solicitudes/" + guardada.getSoliId();
        } catch (SolicitudesService.SolicitudInvalidaException e) {
            model.addAttribute("error", e.getMessage());
            cargarFormulario(model, form, usuarioActual);
            return "solicitudes/formulario";
        }
    }

    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Principal principal, Model model) {
        Usuario usuarioActual = obtenerUsuarioActual(principal);
        Solicitudes solicitud = solicitudesService.buscarPorIdConDetalles(id);
        if (solicitud == null) {
            model.addAttribute("error", "Solicitud no encontrada.");
            return "redirect:/solicitudes";
        }

        if (!puedeVerSolicitud(solicitud, usuarioActual)) {
            model.addAttribute("error", "No tiene permisos para ver esta solicitud.");
            return "redirect:/solicitudes";
        }

        model.addAttribute("solicitud", solicitud);
        model.addAttribute("tipoSolicitudNormalizada", normalizarTexto(solicitud.getTipoSolicitud()));
        model.addAttribute("historial", solicitudesService.obtenerHistorial(id));
        model.addAttribute("usuarioActualAsignado", solicitud.getActivo() != null
            ? solicitudesService.obtenerUsuarioActualAsignado(solicitud.getActivo().getActivoId())
            : null);
        cargarPermisosDetalle(model, solicitud, usuarioActual);
        return "solicitudes/detalle";
    }

    @PostMapping("/{id}/cambiar-estado")
    public String cambiarEstado(@PathVariable Long id,
                                @ModelAttribute SolicitudCambioEstadoDTO cambio,
                                Principal principal,
                                RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.cambiarEstado(id, cambio, usuarioActual);
            flash.addFlashAttribute("success", "Estado actualizado correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/asignar-responsable")
    public String asignarResponsable(@PathVariable Long id,
                                     @RequestParam Long responsableId,
                                     @RequestParam(required = false) String observacion,
                                     Principal principal,
                                     RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.asignarResponsableYEnviarAAnalisis(id, responsableId, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Responsable asignado correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/aprobar")
    public String aprobar(@PathVariable Long id,
                          @RequestParam(required = false) String observacion,
                          Principal principal,
                          RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.aprobarSolicitud(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Solicitud aprobada correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(@PathVariable Long id,
                           @RequestParam String observacion,
                           Principal principal,
                           RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.rechazarSolicitud(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Solicitud rechazada correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/cerrar")
    public String cerrar(@PathVariable Long id,
                         @RequestParam(required = false) String observacion,
                         Principal principal,
                         RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.cerrarSolicitud(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Solicitud cerrada correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/iniciar-operacion")
    public String iniciarOperacion(@PathVariable Long id,
                                   @RequestParam(required = false) String observacion,
                                   Principal principal,
                                   RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.iniciarOperacion(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Operacion iniciada correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/resolver-operacion")
    public String resolverOperacion(@PathVariable Long id,
                                    @RequestParam(required = false) String observacion,
                                    Principal principal,
                                    RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.resolverOperacion(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Operacion resuelta correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/ejecutar-baja")
    public String ejecutarBaja(@PathVariable Long id,
                               @RequestParam(required = false) String observacion,
                               Principal principal,
                               RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.ejecutarBajaSolicitud(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Baja ejecutada correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/ejecutar-asignacion")
    public String ejecutarAsignacion(@PathVariable Long id,
                                     @RequestParam(required = false) String observacion,
                                     Principal principal,
                                     RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.ejecutarAsignacionSolicitud(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Asignacion ejecutada correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/{id}/ejecutar-reasignacion")
    public String ejecutarReasignacion(@PathVariable Long id,
                                       @RequestParam(required = false) String observacion,
                                       Principal principal,
                                       RedirectAttributes flash) {
        try {
            Usuario usuarioActual = obtenerUsuarioActual(principal);
            solicitudesService.ejecutarReasignacionSolicitud(id, usuarioActual, observacion);
            flash.addFlashAttribute("success", "Reasignacion ejecutada correctamente.");
        } catch (SolicitudesService.SolicitudInvalidaException | SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/solicitudes/" + id;
    }

    @GetMapping("/{id}/rechazar")
    public String rechazarForm(@PathVariable Long id, Principal principal, Model model) {
        Usuario usuarioActual = obtenerUsuarioActual(principal);
        Solicitudes solicitud = solicitudesService.buscarPorIdConDetalles(id);
        if (solicitud == null || !puedeVerSolicitud(solicitud, usuarioActual)) {
            return "redirect:/solicitudes";
        }
        model.addAttribute("solicitud", solicitud);
        return "solicitudes/rechazar";
    }

    @GetMapping("/estadisticas")
    public String estadisticas(Principal principal, Model model) {
        Usuario usuarioActual = obtenerUsuarioActual(principal);

        model.addAttribute("totalSolicitudes", solicitudesService.obtenerTodas().size());
        model.addAttribute("pendientes", solicitudesService.contarPendientes());
        model.addAttribute("porEstado", solicitudesService.contarPorEstado());
        model.addAttribute("porTipo", solicitudesService.contarPorTipo());
        model.addAttribute("porUsuario", solicitudesService.contarPorUsuario());
        model.addAttribute("ultimasSolicitudes", solicitudesService.obtenerUltimas(10));
        return "solicitudes/estadisticas";
    }

    @GetMapping("/editar/{id}")
    public String editarNoDisponible(@PathVariable Long id, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "No se permite editar solicitudes enviadas.");
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/actualizar/{id}")
    public String actualizarNoDisponible(@PathVariable Long id, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "No se permite editar solicitudes enviadas.");
        return "redirect:/solicitudes/" + id;
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarNoDisponible(@PathVariable Long id, RedirectAttributes flash) {
        flash.addFlashAttribute("error", "No se permite eliminar solicitudes enviadas.");
        return "redirect:/solicitudes/" + id;
    }

    private void cargarFiltrosListado(Model model, String texto, Long tipoId, Long estadoId,
                                      Long solicitanteId, Long responsableId,
                                      LocalDate fechaDesde, LocalDate fechaHasta) {
        model.addAttribute("texto", texto);
        model.addAttribute("tipoId", tipoId);
        model.addAttribute("estadoId", estadoId);
        model.addAttribute("solicitanteId", solicitanteId);
        model.addAttribute("responsableId", responsableId);
        model.addAttribute("fechaDesde", fechaDesde);
        model.addAttribute("fechaHasta", fechaHasta);
        model.addAttribute("tipos", solicitudesService.obtenerTiposFlujo());
        model.addAttribute("estados", solicitudesService.obtenerEstadosFlujo());
        model.addAttribute("usuarios", solicitudesService.obtenerUsuariosActivos());
        model.addAttribute("responsables", solicitudesService.obtenerResponsablesDisponibles());
    }

    private void cargarFormulario(Model model, SolicitudFormDTO form, Usuario usuarioActual) {
        model.addAttribute("solicitud", form);
        model.addAttribute("tipos", solicitudesService.obtenerTiposFlujo());
        model.addAttribute("estados", solicitudesService.obtenerEstadosFlujo());
        model.addAttribute("activosDisponibles", solicitudesService.obtenerActivosDisponiblesParaSolicitud());
        model.addAttribute("activosReferencia", solicitudesService.obtenerActivosParaReferencia());
        model.addAttribute("activosReferenciaCompra", solicitudesService.obtenerActivosReferencialesConDatosTecnicos());
        model.addAttribute("activosAsignados", solicitudesService.obtenerActivosAsignadosConAsignacionActiva());
        model.addAttribute("marcas", solicitudesService.obtenerMarcasActivas());
        model.addAttribute("usuariosDestinatarios", solicitudesService.obtenerUsuariosDestinatariosDisponibles());
        model.addAttribute("responsables", solicitudesService.obtenerResponsablesDisponibles());
        model.addAttribute("usuariosAsignadosPorActivo", construirMapaUsuariosAsignados());
        model.addAttribute("usuarioActual", usuarioActual);
        model.addAttribute("esSuperAdmin", true);
        model.addAttribute("esTecnico", true);
        model.addAttribute("esStandardUser", false);
        model.addAttribute("estadoPendienteId", solicitudesService.obtenerEstadoIdPorNombre("Pendiente"));
        model.addAttribute("estadoEnAnalisisId", solicitudesService.obtenerEstadoIdPorNombre("En análisis"));
        model.addAttribute("estadoAprobadaId", solicitudesService.obtenerEstadoIdPorNombre("Aprobada"));
        model.addAttribute("estadoEnEjecucionId", solicitudesService.obtenerEstadoIdPorNombre("En ejecución"));
        model.addAttribute("estadoResueltaId", solicitudesService.obtenerEstadoIdPorNombre("Resuelta"));
        model.addAttribute("estadoRechazadaId", solicitudesService.obtenerEstadoIdPorNombre("Rechazada"));
        model.addAttribute("estadoCerradaId", solicitudesService.obtenerEstadoIdPorNombre("Cerrada"));
        model.addAttribute("tipoMantenimientoId", solicitudesService.obtenerTipoIdPorNombre("Mantenimiento"));
        model.addAttribute("tipoBajaId", solicitudesService.obtenerTipoIdPorNombre("Baja"));
        model.addAttribute("tipoCompraId", solicitudesService.obtenerTipoIdPorNombre("Compra"));
        model.addAttribute("tipoAsignacionId", solicitudesService.obtenerTipoIdPorNombre("Asignación"));
        model.addAttribute("tipoReasignacionId", solicitudesService.obtenerTipoIdPorNombre("Reasignación"));
    }

    private void cargarPermisosDetalle(Model model, Solicitudes solicitud, Usuario usuarioActual) {
        model.addAttribute("esSuperAdmin", true);
        model.addAttribute("esTecnico", true);
        model.addAttribute("esPropietario", true);
        model.addAttribute("esResponsable", true);
        model.addAttribute("puedeAsignarResponsable", solicitud.estaPendiente() || solicitud.estaEnAnalisis());
        model.addAttribute("puedePasarAnalisis", solicitud.estaPendiente());
        model.addAttribute("puedeAprobar", solicitud.estaEnAnalisis());
        model.addAttribute("puedeRechazar", solicitud.estaPendiente() || solicitud.estaEnAnalisis());
        model.addAttribute("puedeIniciarOperacion", solicitud.estaAprobada());
        model.addAttribute("puedeResolverOperacion", solicitud.estaEnEjecucion());
        model.addAttribute("puedeCerrar", solicitud.estaResuelta());
        model.addAttribute("puedeVer", true);
        model.addAttribute("responsables", solicitudesService.obtenerResponsablesDisponibles());
        model.addAttribute("estados", solicitudesService.obtenerEstadosFlujo());
        model.addAttribute("estadoPendienteId", solicitudesService.obtenerEstadoIdPorNombre("Pendiente"));
        model.addAttribute("estadoEnAnalisisId", solicitudesService.obtenerEstadoIdPorNombre("En análisis"));
        model.addAttribute("estadoAprobadaId", solicitudesService.obtenerEstadoIdPorNombre("Aprobada"));
        model.addAttribute("estadoEnEjecucionId", solicitudesService.obtenerEstadoIdPorNombre("En ejecución"));
        model.addAttribute("estadoResueltaId", solicitudesService.obtenerEstadoIdPorNombre("Resuelta"));
        model.addAttribute("estadoRechazadaId", solicitudesService.obtenerEstadoIdPorNombre("Rechazada"));
        model.addAttribute("estadoCerradaId", solicitudesService.obtenerEstadoIdPorNombre("Cerrada"));
        model.addAttribute("esMantenimiento", "Mantenimiento".equalsIgnoreCase(solicitud.getTipoSolicitud()));
        model.addAttribute("esBaja", "Baja".equalsIgnoreCase(solicitud.getTipoSolicitud()));
        model.addAttribute("esCompra", "Compra".equalsIgnoreCase(solicitud.getTipoSolicitud()));
        model.addAttribute("esAsignacion", "Asignación".equalsIgnoreCase(solicitud.getTipoSolicitud()));
        model.addAttribute("esReasignacion", "Reasignación".equalsIgnoreCase(solicitud.getTipoSolicitud()));
    }

    private Usuario obtenerUsuarioActual(Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Debe iniciar sesion nuevamente.");
        }
        return usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private boolean puedeVerSolicitud(Solicitudes solicitud, Usuario usuarioActual) {
        return usuarioActual != null;
    }

    private Map<Long, String> construirMapaUsuariosAsignados() {
        Map<Long, String> mapa = new HashMap<>();
        solicitudesService.obtenerActivosAsignadosConAsignacionActiva().forEach(activo -> {
            Usuario usuario = solicitudesService.obtenerUsuarioActualAsignado(activo.getActivoId());
            if (usuario != null) {
                mapa.put(activo.getActivoId(), usuario.getUsuLogin());
            }
        });
        return mapa;
    }

    private String normalizarTexto(String valor) {
        if (valor == null) {
            return "";
        }
        String normalizado = Normalizer.normalize(valor, Normalizer.Form.NFD);
        return normalizado.replaceAll("\\p{M}+", "").trim().toLowerCase();
    }
}
