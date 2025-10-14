package com.sistema.iTsystem.controller;

import java.math.BigDecimal;
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

import com.sistema.iTsystem.model.Mantenimiento;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.MantenimientoService;

@Controller
@RequestMapping("/mantenimientos")
public class MantenimientoController {

    // ==================== SERVICES ====================
    
    @Autowired
    private MantenimientoService mantenimientoService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR MANTENIMIENTOS ====================
    
    @GetMapping
    public String listar(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(required = false) Long hardwareId,
            @RequestParam(required = false) Long tipoId,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) String buscar,
            Model model) {
        
        try {
            Page<Mantenimiento> paginaMantenimientos;
            Pageable pageable = PageRequest.of(pagina, 10);
            
            //Filtros
            if (hardwareId != null) {
                paginaMantenimientos = mantenimientoService.buscarPorHardware(hardwareId, pageable);
                model.addAttribute("filtroActivo", "hardware");
            } else if (tipoId != null) {
                paginaMantenimientos = mantenimientoService.buscarPorTipo(tipoId, pageable);
                model.addAttribute("filtroActivo", "tipo");
            } else {
                paginaMantenimientos = mantenimientoService.obtenerTodos(pageable);
            }
            
            // Si hay filtros sin paginación, convertir a lista
            java.util.List<Mantenimiento> lista = null;
            
            if ("en-curso".equals(filtro)) {
                lista = mantenimientoService.buscarEnCurso();
                model.addAttribute("filtroActivo", "en-curso");
            } else if ("finalizados".equals(filtro)) {
                lista = mantenimientoService.buscarFinalizados();
                model.addAttribute("filtroActivo", "finalizados");
            } else if ("preventivos".equals(filtro)) {
                lista = mantenimientoService.buscarPreventivos();
                model.addAttribute("filtroActivo", "preventivos");
            } else if ("correctivos".equals(filtro)) {
                lista = mantenimientoService.buscarCorrectivos();
                model.addAttribute("filtroActivo", "correctivos");
            } else if ("sin-costo".equals(filtro)) {
                lista = mantenimientoService.buscarSinCosto();
                model.addAttribute("filtroActivo", "sin-costo");
            } else if ("mes-actual".equals(filtro)) {
                lista = mantenimientoService.buscarDelMesActual();
                model.addAttribute("filtroActivo", "mes-actual");
            } else if (buscar != null && !buscar.trim().isEmpty()) {
                lista = mantenimientoService.buscarPorDescripcion(buscar);
                model.addAttribute("filtroActivo", "busqueda");
                model.addAttribute("buscar", buscar);
            }
            
            // Cargar datos según si es paginado o lista
            if (lista != null) {
                model.addAttribute("mantenimientos", lista);
                model.addAttribute("usaLista", true);
            } else {
                model.addAttribute("mantenimientos", paginaMantenimientos.getContent());
                model.addAttribute("paginaActual", pagina);
                model.addAttribute("totalPaginas", paginaMantenimientos.getTotalPages());
                model.addAttribute("totalElementos", paginaMantenimientos.getTotalElements());
                model.addAttribute("usaLista", false);
            }
            
            // Catálogos para filtros
            model.addAttribute("tipos", mantenimientoService.obtenerTodosTipos());
            model.addAttribute("hardware", mantenimientoService.obtenerTodoHardware());
            
            // Estadísticas resumidas
            model.addAttribute("enCurso", mantenimientoService.contarEnCurso());
            model.addAttribute("costoTotal", mantenimientoService.calcularCostoTotal());
            
            return "mantenimientos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar mantenimientos: " + e.getMessage());
            return "mantenimientos/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            //Obtener mantenimiento con todas las relaciones
            Mantenimiento mantenimiento = mantenimientoService.buscarPorIdConDetalles(id);
            
            if (mantenimiento == null) {
                throw new RuntimeException("Mantenimiento no encontrado");
            }
            
            model.addAttribute("mantenimiento", mantenimiento);
            
            //Información adicional
            model.addAttribute("estaFinalizado", mantenimiento.getMantFechaFin() != null);
            
            // Calcular duración si está finalizado
            if (mantenimiento.getMantFechaFin() != null) {
                long dias = java.time.temporal.ChronoUnit.DAYS.between(
                    mantenimiento.getMantFechaIni(), 
                    mantenimiento.getMantFechaFin()
                );
                model.addAttribute("duracionDias", dias);
            } else {
                // Calcular días transcurridos
                long diasTranscurridos = java.time.temporal.ChronoUnit.DAYS.between(
                    mantenimiento.getMantFechaIni(), 
                    LocalDate.now()
                );
                model.addAttribute("diasTranscurridos", diasTranscurridos);
            }
            
            return "mantenimientos/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        }
    }

    // ==================== FORMULARIO NUEVO ====================
    
    @GetMapping("/nuevo")
    public String nuevoForm(
            @RequestParam(required = false) Long hardwareId,
            Model model) {
        
        try {
            Mantenimiento mantenimiento = new Mantenimiento();
            
            // Si viene hardwareId, pre-seleccionar
            if (hardwareId != null) {
                mantenimientoService.obtenerTodoHardware().stream()
                    .filter(hw -> hw.getHwId().equals(hardwareId))
                    .findFirst()
                    .ifPresent(hw -> {
                        mantenimiento.setHardwareInfo(hw);
                        model.addAttribute("hardwareSeleccionado", hw);
                    });
            }
            
            model.addAttribute("mantenimiento", mantenimiento);
            
            //Catálogos
            model.addAttribute("tipos", mantenimientoService.obtenerTodosTipos());
            model.addAttribute("hardware", mantenimientoService.obtenerTodoHardware());
            
            return "mantenimientos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        }
    }

    // ==================== GUARDAR MANTENIMIENTO ====================
    
    @PostMapping("/guardar")
    public String guardar(Mantenimiento mantenimiento, RedirectAttributes flash) {
        try {
            //Delegado al service
            Mantenimiento mantenimientoGuardado = mantenimientoService.crear(mantenimiento);
            
            flash.addFlashAttribute("success", 
                "Mantenimiento registrado exitosamente para el hardware: " + 
                mantenimientoGuardado.getHardwareInfo().getActivo().getActivoNom());
            
            return "redirect:/mantenimientos/" + mantenimientoGuardado.getMantId();
            
        } catch (MantenimientoService.MantenimientoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/mantenimientos/nuevo";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/mantenimientos/nuevo";
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            Mantenimiento mantenimiento = mantenimientoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Mantenimiento no encontrado"));
            
            model.addAttribute("mantenimiento", mantenimiento);
            model.addAttribute("tipos", mantenimientoService.obtenerTodosTipos());
            model.addAttribute("hardware", mantenimientoService.obtenerTodoHardware());
            
            return "mantenimientos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        }
    }

    // ==================== ACTUALIZAR MANTENIMIENTO ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id, 
            Mantenimiento mantenimiento, 
            RedirectAttributes flash) {
        
        try {
            //Delegado al service
            Mantenimiento mantenimientoActualizado = mantenimientoService.actualizar(id, mantenimiento);
            
            flash.addFlashAttribute("success", "Mantenimiento actualizado exitosamente");
            
            return "redirect:/mantenimientos/" + mantenimientoActualizado.getMantId();
            
        } catch (MantenimientoService.MantenimientoNoEncontradoException | 
                 MantenimientoService.MantenimientoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/mantenimientos/editar/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/mantenimientos/editar/" + id;
        }
    }

    // ==================== FINALIZAR MANTENIMIENTO ====================
    
    @GetMapping("/{id}/finalizar")
    public String finalizarForm(@PathVariable Long id, Model model) {
        try {
            Mantenimiento mantenimiento = mantenimientoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Mantenimiento no encontrado"));
            
            if (mantenimiento.getMantFechaFin() != null) {
                throw new RuntimeException("El mantenimiento ya está finalizado");
            }
            
            model.addAttribute("mantenimiento", mantenimiento);
            model.addAttribute("fechaHoy", LocalDate.now());
            
            return "mantenimientos/finalizar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos/" + id;
        }
    }

    @PostMapping("/{id}/finalizar")
    public String finalizar(
            @PathVariable Long id,
            @RequestParam(required = false) LocalDate fechaFin,
            RedirectAttributes flash) {
        
        try {
            //Delegado al service
            mantenimientoService.finalizar(id, fechaFin);
            
            flash.addFlashAttribute("success", "Mantenimiento finalizado exitosamente");
            return "redirect:/mantenimientos/" + id;
            
        } catch (MantenimientoService.MantenimientoYaFinalizadoException |
                 MantenimientoService.MantenimientoInvalidoException |
                 MantenimientoService.MantenimientoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/mantenimientos/" + id;
        }
    }

    // ==================== ELIMINAR MANTENIMIENTO ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id, 
            Principal principal, 
            RedirectAttributes flash) {
        
        try {
            //Validar que sea administrador
            validarEsAdministrador(principal);
            
            // Delegado al service
            mantenimientoService.eliminar(id);
            
            flash.addFlashAttribute("success", "Mantenimiento eliminado exitosamente");
            return "redirect:/mantenimientos";
            
        } catch (MantenimientoService.MantenimientoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/mantenimientos/" + id;
        }
    }

    // ==================== FILTROS RÁPIDOS ====================
    
    @GetMapping("/en-curso")
    public String listarEnCurso(Model model) {
        return "redirect:/mantenimientos?filtro=en-curso";
    }

    @GetMapping("/finalizados")
    public String listarFinalizados(Model model) {
        return "redirect:/mantenimientos?filtro=finalizados";
    }

    @GetMapping("/preventivos")
    public String listarPreventivos(Model model) {
        return "redirect:/mantenimientos?filtro=preventivos";
    }

    @GetMapping("/correctivos")
    public String listarCorrectivos(Model model) {
        return "redirect:/mantenimientos?filtro=correctivos";
    }

    @GetMapping("/sin-costo")
    public String listarSinCosto(Model model) {
        return "redirect:/mantenimientos?filtro=sin-costo";
    }

    @GetMapping("/mes-actual")
    public String listarMesActual(Model model) {
        return "redirect:/mantenimientos?filtro=mes-actual";
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================
    
    @GetMapping("/por-hardware/{hardwareId}")
    public String listarPorHardware(@PathVariable Long hardwareId, Model model) {
        return "redirect:/mantenimientos?hardwareId=" + hardwareId;
    }

    @GetMapping("/por-tipo/{tipoId}")
    public String listarPorTipo(@PathVariable Long tipoId, Model model) {
        return "redirect:/mantenimientos?tipoId=" + tipoId;
    }

    @GetMapping("/por-fechas")
    public String buscarPorFechas(
            @RequestParam LocalDate fechaInicio,
            @RequestParam LocalDate fechaFin,
            Model model) {
        
        try {
            model.addAttribute("mantenimientos", 
                mantenimientoService.buscarPorFechas(fechaInicio, fechaFin));
            model.addAttribute("filtroActivo", "por-fechas");
            model.addAttribute("fechaInicio", fechaInicio);
            model.addAttribute("fechaFin", fechaFin);
            model.addAttribute("usaLista", true);
            
            return "mantenimientos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        }
    }

    @GetMapping("/mas-costosos")
    public String listarMasCostosos(Model model) {
        try {
            model.addAttribute("mantenimientos", mantenimientoService.buscarMasCostosos());
            model.addAttribute("filtroActivo", "mas-costosos");
            model.addAttribute("usaLista", true);
            
            return "mantenimientos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        }
    }

    @GetMapping("/por-costo")
    public String buscarPorCosto(
            @RequestParam BigDecimal costoMinimo,
            Model model) {
        
        try {
            model.addAttribute("mantenimientos", 
                mantenimientoService.buscarPorCostoMayorA(costoMinimo));
            model.addAttribute("filtroActivo", "por-costo");
            model.addAttribute("costoMinimo", costoMinimo);
            model.addAttribute("usaLista", true);
            
            return "mantenimientos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        }
    }

    // ==================== ESTADÍSTICAS ====================
    
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        try {
            model.addAttribute("totalMantenimientos", mantenimientoService.obtenerTodos().size());
            model.addAttribute("enCurso", mantenimientoService.contarEnCurso());
            model.addAttribute("porTipo", mantenimientoService.contarPorTipo());
            model.addAttribute("porHardware", mantenimientoService.contarPorHardware());
            model.addAttribute("porMes", mantenimientoService.contarPorMes());
            model.addAttribute("costoTotal", mantenimientoService.calcularCostoTotal());
            model.addAttribute("costoPromedio", mantenimientoService.calcularCostoPromedio());
            model.addAttribute("sinCosto", mantenimientoService.contarSinCosto());
            
            // Listas útiles
            model.addAttribute("mantenimientosEnCurso", mantenimientoService.buscarEnCurso());
            model.addAttribute("ultimosMantenimientos", mantenimientoService.obtenerUltimos(10));
            model.addAttribute("masCostosos", mantenimientoService.buscarMasCostosos());
            
            return "mantenimientos/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
        }
    }

    // ==================== REPORTES ====================
    
    @GetMapping("/reporte-hardware/{hardwareId}")
    public String reporteHardware(@PathVariable Long hardwareId, Model model) {
        try {
            model.addAttribute("mantenimientos", 
                mantenimientoService.buscarPorHardwareConDetalles(hardwareId));
            model.addAttribute("costoTotal", 
                mantenimientoService.calcularCostoPorHardware(hardwareId));
            model.addAttribute("totalMantenimientos", 
                mantenimientoService.contarPorHardwareId(hardwareId));
            
            // Obtener info del hardware
            mantenimientoService.obtenerTodoHardware().stream()
                .filter(hw -> hw.getHwId().equals(hardwareId))
                .findFirst()
                .ifPresent(hw -> model.addAttribute("hardware", hw));
            
            return "mantenimientos/reporte-hardware";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/mantenimientos";
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