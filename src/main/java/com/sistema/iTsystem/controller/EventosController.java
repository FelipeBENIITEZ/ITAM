package com.sistema.iTsystem.controller;

import java.security.Principal;
import java.time.LocalDate;

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

import com.sistema.iTsystem.model.Eventos;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.EventosService;

@Controller
@RequestMapping("/eventos")
public class EventosController {

    // ==================== SERVICES ====================
    
    @Autowired
    private EventosService eventosService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR EVENTOS ====================
    
    @GetMapping
    public String listar(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(required = false) Long activoId,
            @RequestParam(required = false) Long nivelId,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) String buscar,
            Model model) {
        
        try {
            Page<Eventos> paginaEventos;
            Pageable pageable = PageRequest.of(pagina, 15);
            
            // ✅ Filtros con paginación
            if (activoId != null) {
                paginaEventos = eventosService.buscarPorActivo(activoId, pageable);
                model.addAttribute("filtroActivo", "activo");
            } else if (nivelId != null) {
                paginaEventos = eventosService.buscarPorNivel(nivelId, pageable);
                model.addAttribute("filtroActivo", "nivel");
            } else {
                paginaEventos = eventosService.obtenerTodos(pageable);
            }
            
            // Filtros sin paginación
            java.util.List<Eventos> lista = null;
            
            if ("criticos".equals(filtro)) {
                lista = eventosService.buscarCriticos();
                model.addAttribute("filtroActivo", "criticos");
            } else if ("hoy".equals(filtro)) {
                lista = eventosService.buscarDeHoy();
                model.addAttribute("filtroActivo", "hoy");
            } else if ("ultima-semana".equals(filtro)) {
                lista = eventosService.buscarUltimaSemana();
                model.addAttribute("filtroActivo", "ultima-semana");
            } else if ("mes-actual".equals(filtro)) {
                lista = eventosService.buscarDelMesActual();
                model.addAttribute("filtroActivo", "mes-actual");
            } else if ("con-impacto".equals(filtro)) {
                lista = eventosService.buscarConImpacto();
                model.addAttribute("filtroActivo", "con-impacto");
            } else if ("sin-impacto".equals(filtro)) {
                lista = eventosService.buscarSinImpacto();
                model.addAttribute("filtroActivo", "sin-impacto");
            } else if (buscar != null && !buscar.trim().isEmpty()) {
                lista = eventosService.buscarPorDescripcion(buscar);
                model.addAttribute("filtroActivo", "busqueda");
                model.addAttribute("buscar", buscar);
            }
            
            // Cargar datos
            if (lista != null) {
                model.addAttribute("eventos", lista);
                model.addAttribute("usaLista", true);
            } else {
                model.addAttribute("eventos", paginaEventos.getContent());
                model.addAttribute("paginaActual", pagina);
                model.addAttribute("totalPaginas", paginaEventos.getTotalPages());
                model.addAttribute("totalElementos", paginaEventos.getTotalElements());
                model.addAttribute("usaLista", false);
            }
            
            // Catálogos para filtros
            model.addAttribute("niveles", eventosService.obtenerTodosNiveles());
            model.addAttribute("activos", eventosService.obtenerTodosActivos());
            
            // Estadísticas resumidas
            model.addAttribute("criticos", eventosService.contarCriticos());
            
            return "eventos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar eventos: " + e.getMessage());
            return "eventos/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            //Obtener evento con todas las relaciones
            Eventos evento = eventosService.buscarPorIdConDetalles(id);
            
            if (evento == null) {
                throw new RuntimeException("Evento no encontrado");
            }
            
            model.addAttribute("evento", evento);
            
            // Información adicional
            model.addAttribute("tieneImpacto", 
                evento.getEventImpacto() != null && !evento.getEventImpacto().trim().isEmpty());
            
            return "eventos/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/eventos";
        }
    }

    // ==================== FORMULARIO NUEVO ====================
    
    @GetMapping("/nuevo")
    public String nuevoForm(
            @RequestParam(required = false) Long activoId,
            Model model) {
        
        try {
            Eventos evento = new Eventos();
            
            // Si viene activoId, pre-seleccionar
            if (activoId != null) {
                eventosService.obtenerTodosActivos().stream()
                    .filter(activo -> activo.getActivoId().equals(activoId))
                    .findFirst()
                    .ifPresent(activo -> {
                        evento.setActivo(activo);
                        model.addAttribute("activoSeleccionado", activo);
                    });
            }
            
            model.addAttribute("evento", evento);
            
            //Catálogos
            model.addAttribute("niveles", eventosService.obtenerTodosNiveles());
            model.addAttribute("activos", eventosService.obtenerTodosActivos());
            
            return "eventos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/eventos";
        }
    }

    // ==================== GUARDAR EVENTO ====================
    
    @PostMapping("/guardar")
    public String guardar(Eventos evento, RedirectAttributes flash) {
        try {
            //Delegado al service
            Eventos eventoGuardado = eventosService.crear(evento);
            
            flash.addFlashAttribute("success", 
                "Evento registrado exitosamente para el activo: " + 
                eventoGuardado.getActivo().getActivoNom());
            
            return "redirect:/eventos/" + eventoGuardado.getEventId();
            
        } catch (EventosService.EventoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/eventos/nuevo";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/eventos/nuevo";
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            Eventos evento = eventosService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));
            
            model.addAttribute("evento", evento);
            model.addAttribute("niveles", eventosService.obtenerTodosNiveles());
            model.addAttribute("activos", eventosService.obtenerTodosActivos());
            
            return "eventos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/eventos";
        }
    }

    // ==================== ACTUALIZAR EVENTO ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id, 
            Eventos evento, 
            RedirectAttributes flash) {
        
        try {
            //Delegado al service
            Eventos eventoActualizado = eventosService.actualizar(id, evento);
            
            flash.addFlashAttribute("success", "Evento actualizado exitosamente");
            
            return "redirect:/eventos/" + eventoActualizado.getEventId();
            
        } catch (EventosService.EventoNoEncontradoException | 
                 EventosService.EventoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/eventos/editar/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/eventos/editar/" + id;
        }
    }

    // ==================== ELIMINAR EVENTO ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id, 
            Principal principal, 
            RedirectAttributes flash) {
        
        try {
            //Validar que sea administrador
            validarEsAdministrador(principal);
            
            //Delegado al service
            eventosService.eliminar(id);
            
            flash.addFlashAttribute("success", "Evento eliminado exitosamente");
            return "redirect:/eventos";
            
        } catch (EventosService.EventoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/eventos";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/eventos/" + id;
        }
    }

    // ==================== FILTROS RÁPIDOS ====================
    
    @GetMapping("/criticos")
    public String listarCriticos(Model model) {
        return "redirect:/eventos?filtro=criticos";
    }

    @GetMapping("/hoy")
    public String listarHoy(Model model) {
        return "redirect:/eventos?filtro=hoy";
    }

    @GetMapping("/ultima-semana")
    public String listarUltimaSemana(Model model) {
        return "redirect:/eventos?filtro=ultima-semana";
    }

    @GetMapping("/mes-actual")
    public String listarMesActual(Model model) {
        return "redirect:/eventos?filtro=mes-actual";
    }

    @GetMapping("/con-impacto")
    public String listarConImpacto(Model model) {
        return "redirect:/eventos?filtro=con-impacto";
    }

    @GetMapping("/sin-impacto")
    public String listarSinImpacto(Model model) {
        return "redirect:/eventos?filtro=sin-impacto";
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================
    
    @GetMapping("/por-activo/{activoId}")
    public String listarPorActivo(@PathVariable Long activoId, Model model) {
        return "redirect:/eventos?activoId=" + activoId;
    }

    @GetMapping("/por-nivel/{nivelId}")
    public String listarPorNivel(@PathVariable Long nivelId, Model model) {
        return "redirect:/eventos?nivelId=" + nivelId;
    }

    @GetMapping("/por-fechas")
    public String buscarPorFechas(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin,
            Model model) {
        
        try {
            model.addAttribute("eventos", 
                eventosService.buscarPorFechas(fechaInicio, fechaFin));
            model.addAttribute("filtroActivo", "por-fechas");
            model.addAttribute("fechaInicio", fechaInicio);
            model.addAttribute("fechaFin", fechaFin);
            model.addAttribute("usaLista", true);
            
            return "eventos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/eventos";
        }
    }

    // ==================== ESTADÍSTICAS ====================
    
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        try {
            model.addAttribute("totalEventos", eventosService.obtenerTodos().size());
            model.addAttribute("criticos", eventosService.contarCriticos());
            model.addAttribute("porNivel", eventosService.contarPorNivel());
            model.addAttribute("porActivo", eventosService.contarPorActivo());
            model.addAttribute("porMes", eventosService.contarPorMes());
            
            // Listas útiles
            model.addAttribute("eventosCriticos", eventosService.buscarCriticos());
            model.addAttribute("eventosDeHoy", eventosService.buscarDeHoy());
            model.addAttribute("ultimosEventos", eventosService.obtenerUltimos(20));
            
            return "eventos/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/eventos";
        }
    }

    // ==================== REPORTES ====================
    
    @GetMapping("/reporte-activo/{activoId}")
    public String reporteActivo(@PathVariable Long activoId, Model model) {
        try {
            model.addAttribute("eventos", 
                eventosService.buscarPorActivoConDetalles(activoId));
            model.addAttribute("totalEventos", 
                eventosService.contarPorActivoId(activoId));
            
            // Obtener info del activo
            eventosService.obtenerTodosActivos().stream()
                .filter(activo -> activo.getActivoId().equals(activoId))
                .findFirst()
                .ifPresent(activo -> model.addAttribute("activo", activo));
            
            return "eventos/reporte-activo";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/eventos";
        }
    }

    @GetMapping("/timeline/{activoId}")
    public String timelineActivo(@PathVariable Long activoId, Model model) {
        try {
            // Timeline de eventos de un activo
            model.addAttribute("eventos", 
                eventosService.buscarPorActivoConDetalles(activoId));
            
            eventosService.obtenerTodosActivos().stream()
                .filter(activo -> activo.getActivoId().equals(activoId))
                .findFirst()
                .ifPresent(activo -> model.addAttribute("activo", activo));
            
            return "eventos/timeline";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/eventos";
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