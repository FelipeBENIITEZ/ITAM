package com.sistema.iTsystem.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sistema.iTsystem.model.Solicitudes;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.SolicitudesService;

@Controller
@RequestMapping("/solicitudes")
public class SolicitudesController {

    // ==================== SERVICES ====================
    
    @Autowired
    private SolicitudesService solicitudesService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR SOLICITUDES ====================
    
    @GetMapping
    public String listar(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) String buscar,
            Principal principal,
            Model model) {
        
        try {
            Usuario usuarioActual = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            boolean esAdmin = usuarioActual.getRol().getRolId().equals(1L);
            
            Page<Solicitudes> paginaSolicitudes;
            Pageable pageable = PageRequest.of(pagina, 10);
            
            //Filtros
            if (usuarioId != null && esAdmin) {
                // Solo admin puede ver solicitudes de otros usuarios
                paginaSolicitudes = solicitudesService.buscarPorUsuario(usuarioId, pageable);
                model.addAttribute("filtroActivo", "usuario");
            } else if (estadoId != null) {
                paginaSolicitudes = solicitudesService.buscarPorEstado(estadoId, pageable);
                model.addAttribute("filtroActivo", "estado");
            } else if (tipoId != null) {
                paginaSolicitudes = solicitudesService.buscarPorTipo(tipoId, pageable);
                model.addAttribute("filtroActivo", "tipo");
            } else if (!esAdmin) {
                // Usuario normal solo ve sus propias solicitudes
                paginaSolicitudes = solicitudesService.buscarPorUsuario(
                    usuarioActual.getUsuId(), pageable);
                model.addAttribute("filtroActivo", "mis-solicitudes");
            } else {
                paginaSolicitudes = solicitudesService.obtenerTodas(pageable);
            }
            
            // Filtros sin paginación
            java.util.List<Solicitudes> lista = null;
            
            if ("pendientes".equals(filtro)) {
                lista = solicitudesService.buscarPendientes();
                model.addAttribute("filtroActivo", "pendientes");
            } else if ("aprobadas".equals(filtro)) {
                lista = solicitudesService.buscarAprobadas();
                model.addAttribute("filtroActivo", "aprobadas");
            } else if ("rechazadas".equals(filtro)) {
                lista = solicitudesService.buscarRechazadas();
                model.addAttribute("filtroActivo", "rechazadas");
            } else if (buscar != null && !buscar.trim().isEmpty()) {
                lista = solicitudesService.buscarPorDescripcion(buscar);
                model.addAttribute("filtroActivo", "busqueda");
                model.addAttribute("buscar", buscar);
            }
            
            // Cargar datos
            if (lista != null) {
                model.addAttribute("solicitudes", lista);
                model.addAttribute("usaLista", true);
            } else {
                model.addAttribute("solicitudes", paginaSolicitudes.getContent());
                model.addAttribute("paginaActual", pagina);
                model.addAttribute("totalPaginas", paginaSolicitudes.getTotalPages());
                model.addAttribute("totalElementos", paginaSolicitudes.getTotalElements());
                model.addAttribute("usaLista", false);
            }
            
            // Catálogos
            model.addAttribute("estados", solicitudesService.obtenerTodosEstados());
            model.addAttribute("tipos", solicitudesService.obtenerTodosTipos());
            
            if (esAdmin) {
                model.addAttribute("usuarios", solicitudesService.obtenerTodosUsuarios());
            }
            
            // Estadísticas
            model.addAttribute("pendientes", solicitudesService.contarPendientes());
            model.addAttribute("esAdmin", esAdmin);
            
            return "solicitudes/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar solicitudes: " + e.getMessage());
            return "solicitudes/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Principal principal, Model model) {
        try {
            Usuario usuarioActual = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            Solicitudes solicitud = solicitudesService.buscarPorIdConDetalles(id);
            
            if (solicitud == null) {
                throw new RuntimeException("Solicitud no encontrada");
            }
            
            // Validar que el usuario tenga permiso para ver la solicitud
            boolean esAdmin = usuarioActual.getRol().getRolId().equals(1L);
            boolean esPropietario = solicitud.getUsuario().getUsuId().equals(usuarioActual.getUsuId());
            
            if (!esAdmin && !esPropietario) {
                throw new RuntimeException("No tiene permisos para ver esta solicitud");
            }
            
            model.addAttribute("solicitud", solicitud);
            model.addAttribute("esAdmin", esAdmin);
            model.addAttribute("esPropietario", esPropietario);
            
            // Información de estados
            model.addAttribute("estaPendiente", solicitud.estaPendiente());
            model.addAttribute("estaAprobada", solicitud.estaAprobada());
            model.addAttribute("estaRechazada", solicitud.estaRechazada());
            
            return "solicitudes/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/solicitudes";
        }
    }

    // ==================== FORMULARIO NUEVA ====================
    
    @GetMapping("/nueva")
    public String nuevaForm(Model model) {
        try {
            model.addAttribute("solicitud", new Solicitudes());
            
            //Catálogos
            model.addAttribute("tipos", solicitudesService.obtenerTodosTipos());
            
            return "solicitudes/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/solicitudes";
        }
    }

    // ==================== GUARDAR SOLICITUD ====================
    
    @PostMapping("/guardar")
    public String guardar(
            Solicitudes solicitud, 
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            //Delegado al service (asigna usuario y estado automáticamente)
            Solicitudes solicitudGuardada = solicitudesService.crear(solicitud, usuario);
            
            flash.addFlashAttribute("success", 
                "Solicitud creada exitosamente. Estado: Pendiente");
            
            return "redirect:/solicitudes/" + solicitudGuardada.getSoliId();
            
        } catch (SolicitudesService.SolicitudInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes/nueva";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/solicitudes/nueva";
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Principal principal, Model model) {
        try {
            Usuario usuarioActual = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            Solicitudes solicitud = solicitudesService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            
            // Solo el propietario puede editar su solicitud (y solo si está pendiente)
            if (!solicitud.getUsuario().getUsuId().equals(usuarioActual.getUsuId())) {
                throw new RuntimeException("No tiene permisos para editar esta solicitud");
            }
            
            if (!solicitud.estaPendiente()) {
                throw new RuntimeException("Solo se pueden editar solicitudes pendientes");
            }
            
            model.addAttribute("solicitud", solicitud);
            model.addAttribute("tipos", solicitudesService.obtenerTodosTipos());
            
            return "solicitudes/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/solicitudes";
        }
    }

    // ==================== ACTUALIZAR SOLICITUD ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id, 
            Solicitudes solicitud,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            Usuario usuarioActual = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            Solicitudes solicitudExistente = solicitudesService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            
            // Validar permisos
            if (!solicitudExistente.getUsuario().getUsuId().equals(usuarioActual.getUsuId())) {
                throw new RuntimeException("No tiene permisos para editar esta solicitud");
            }
            
            //Delegado al service
            Solicitudes solicitudActualizada = solicitudesService.actualizar(id, solicitud);
            
            flash.addFlashAttribute("success", "Solicitud actualizada exitosamente");
            
            return "redirect:/solicitudes/" + solicitudActualizada.getSoliId();
            
        } catch (SolicitudesService.SolicitudNoEncontradaException | 
                 SolicitudesService.SolicitudInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes/editar/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/solicitudes/editar/" + id;
        }
    }

    // ==================== APROBAR SOLICITUD (SOLO ADMIN) ====================
    
    @PostMapping("/{id}/aprobar")
    public String aprobar(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            //Validar que sea administrador
            validarEsAdministrador(principal);
            
            //Delegado al service
            solicitudesService.aprobar(id);
            
            flash.addFlashAttribute("success", "Solicitud aprobada exitosamente");
            return "redirect:/solicitudes/" + id;
            
        } catch (SolicitudesService.CambioEstadoInvalidoException |
                 SolicitudesService.SolicitudNoEncontradaException |
                 SolicitudesService.SolicitudInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes/" + id;
        }
    }

    // ==================== RECHAZAR SOLICITUD (SOLO ADMIN) ====================
    
    @GetMapping("/{id}/rechazar")
    public String rechazarForm(@PathVariable Long id, Principal principal, Model model) {
        try {
            validarEsAdministrador(principal);
            
            Solicitudes solicitud = solicitudesService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            
            if (!solicitud.estaPendiente()) {
                throw new RuntimeException("Solo se pueden rechazar solicitudes pendientes");
            }
            
            model.addAttribute("solicitud", solicitud);
            
            return "solicitudes/rechazar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/solicitudes/" + id;
        }
    }

    @PostMapping("/{id}/rechazar")
    public String rechazar(
            @PathVariable Long id,
            @RequestParam String motivo,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            validarEsAdministrador(principal);
            
            //Delegado al service
            solicitudesService.rechazar(id, motivo);
            
            flash.addFlashAttribute("success", "Solicitud rechazada");
            return "redirect:/solicitudes/" + id;
            
        } catch (SolicitudesService.CambioEstadoInvalidoException |
                 SolicitudesService.SolicitudNoEncontradaException |
                 SolicitudesService.SolicitudInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes/" + id;
        }
    }

    // ==================== COMPLETAR SOLICITUD (SOLO ADMIN) ====================
    
    @PostMapping("/{id}/completar")
    public String completar(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            validarEsAdministrador(principal);
            
            //Delegado al service
            solicitudesService.completar(id);
            
            flash.addFlashAttribute("success", "Solicitud completada exitosamente");
            return "redirect:/solicitudes/" + id;
            
        } catch (SolicitudesService.CambioEstadoInvalidoException |
                 SolicitudesService.SolicitudNoEncontradaException |
                 SolicitudesService.SolicitudInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes/" + id;
        }
    }

    // ==================== ELIMINAR SOLICITUD ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id, 
            Principal principal, 
            RedirectAttributes flash) {
        
        try {
            Usuario usuarioActual = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            Solicitudes solicitud = solicitudesService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));
            
            // Admin puede eliminar cualquiera, usuario solo sus propias pendientes
            boolean esAdmin = usuarioActual.getRol().getRolId().equals(1L);
            boolean esPropietario = solicitud.getUsuario().getUsuId().equals(usuarioActual.getUsuId());
            
            if (!esAdmin && !esPropietario) {
                throw new RuntimeException("No tiene permisos para eliminar esta solicitud");
            }
            
            if (!esAdmin && !solicitud.estaPendiente()) {
                throw new RuntimeException("Solo puede eliminar sus solicitudes pendientes");
            }
            
            //Delegado al service
            solicitudesService.eliminar(id);
            
            flash.addFlashAttribute("success", "Solicitud eliminada exitosamente");
            return "redirect:/solicitudes";
            
        } catch (SolicitudesService.SolicitudNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/solicitudes/" + id;
        }
    }

    // ==================== FILTROS RÁPIDOS ====================
    
    @GetMapping("/pendientes")
    public String listarPendientes(Model model) {
        return "redirect:/solicitudes?filtro=pendientes";
    }

    @GetMapping("/aprobadas")
    public String listarAprobadas(Model model) {
        return "redirect:/solicitudes?filtro=aprobadas";
    }

    @GetMapping("/rechazadas")
    public String listarRechazadas(Model model) {
        return "redirect:/solicitudes?filtro=rechazadas";
    }

    @GetMapping("/mis-solicitudes")
    public String misSolicitudes(Principal principal, Model model) {
        try {
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            return "redirect:/solicitudes?usuarioId=" + usuario.getUsuId();
            
        } catch (Exception e) {
            return "redirect:/solicitudes";
        }
    }

    // ==================== ESTADÍSTICAS (SOLO ADMIN) ====================
    
    @GetMapping("/estadisticas")
    public String estadisticas(Principal principal, Model model) {
        try {
            validarEsAdministrador(principal);
            
            model.addAttribute("totalSolicitudes", solicitudesService.obtenerTodas().size());
            model.addAttribute("pendientes", solicitudesService.contarPendientes());
            model.addAttribute("porEstado", solicitudesService.contarPorEstado());
            model.addAttribute("porTipo", solicitudesService.contarPorTipo());
            model.addAttribute("porUsuario", solicitudesService.contarPorUsuario());
            
            // Listas
            model.addAttribute("solicitudesPendientes", solicitudesService.buscarPendientes());
            model.addAttribute("ultimasSolicitudes", solicitudesService.obtenerUltimas(10));
            
            return "solicitudes/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/solicitudes";
        }
    }

    // ==================== MÉTODOS HELPER ====================
    
    /**
     * Validar que el usuario sea administrador (rol_id = 1)
     */
    private void validarEsAdministrador(Principal principal) {
        Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!usuario.getRol().getRolId().equals(1L)) {
            throw new RuntimeException("Esta acción requiere privilegios de Administrador");
        }
    }
}