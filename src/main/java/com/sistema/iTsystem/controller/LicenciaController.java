package com.sistema.iTsystem.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sistema.iTsystem.model.LicenciaInfo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.LicenciaInfoService;
import com.sistema.iTsystem.service.SoftwareInfoService;

@Controller
@RequestMapping("/licencias")
public class LicenciaController {

    // ==================== SERVICES ====================
    
    @Autowired
    private LicenciaInfoService licenciaService;
    
    @Autowired
    private SoftwareInfoService softwareService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR LICENCIAS ====================
    
    @GetMapping
    public String listar(
            @RequestParam(required = false) Long softwareId,
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) String filtro,
            Model model) {
        
        try {
            java.util.List<LicenciaInfo> licencias;
            
            //Filtros
            if (softwareId != null) {
                licencias = licenciaService.buscarPorSoftware(softwareId);
                model.addAttribute("filtroActivo", "software");
            } else if (estadoId != null) {
                licencias = licenciaService.buscarPorEstado(estadoId);
                model.addAttribute("filtroActivo", "estado");
            } else if ("vencidas".equals(filtro)) {
                licencias = licenciaService.buscarVencidas();
                model.addAttribute("filtroActivo", "vencidas");
            } else if ("proximas-vencer".equals(filtro)) {
                licencias = licenciaService.buscarProximasAVencer();
                model.addAttribute("filtroActivo", "proximas-vencer");
            } else if ("activas".equals(filtro)) {
                licencias = licenciaService.buscarActivas();
                model.addAttribute("filtroActivo", "activas");
            } else if ("sin-cupos".equals(filtro)) {
                licencias = licenciaService.buscarSinCupos();
                model.addAttribute("filtroActivo", "sin-cupos");
            } else if ("con-cupos".equals(filtro)) {
                licencias = licenciaService.buscarConCuposDisponibles();
                model.addAttribute("filtroActivo", "con-cupos");
            } else {
                licencias = licenciaService.obtenerTodas();
            }
            
            model.addAttribute("licencias", licencias);
            
            // Catálogos para filtros
            model.addAttribute("estados", licenciaService.obtenerTodosEstados());
            model.addAttribute("software", licenciaService.obtenerTodosSoftware());
            
            // Estadísticas resumidas
            model.addAttribute("totalLicencias", licencias.size());
            model.addAttribute("vencidas", licenciaService.contarVencidas());
            model.addAttribute("proximasVencer", licenciaService.contarProximasAVencer());
            model.addAttribute("cuposDisponibles", licenciaService.contarCuposDisponibles());
            
            return "licencias/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar licencias: " + e.getMessage());
            return "licencias/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            //Obtener licencia con todas las relaciones
            LicenciaInfo licencia = licenciaService.buscarPorIdConDetalles(id);
            
            if (licencia == null) {
                throw new RuntimeException("Licencia no encontrada");
            }
            
            model.addAttribute("licencia", licencia);
            
            //Información adicional
            model.addAttribute("cuposDisponibles", licencia.getCuposDisponibles());
            model.addAttribute("estaVencida", licencia.estaVencida());
            model.addAttribute("tieneCuposDisponibles", licencia.tieneCuposDisponibles());
            model.addAttribute("puedeSerUsada", licenciaService.puedeSerUsada(id));
            
            // Calcular días hasta vencer
            if (licencia.getLicenciaFin() != null && !licencia.estaVencida()) {
                long diasHastaVencer = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), 
                    licencia.getLicenciaFin()
                );
                model.addAttribute("diasHastaVencer", diasHastaVencer);
            }
            
            return "licencias/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/licencias";
        }
    }

    // ==================== FORMULARIO NUEVO ====================
    
    @GetMapping("/nuevo")
    public String nuevoForm(
            @RequestParam(required = false) Long softwareId,
            Model model) {
        
        try {
            LicenciaInfo licencia = new LicenciaInfo();
            
            // Si viene softwareId, pre-seleccionar
            if (softwareId != null) {
                softwareService.buscarPorId(softwareId).ifPresent(software -> {
                    licencia.setSoftwareInfo(software);
                    model.addAttribute("softwareSeleccionado", software);
                });
            }
            
            model.addAttribute("licencia", licencia);
            
            //Catálogos
            model.addAttribute("software", licenciaService.obtenerTodosSoftware());
            model.addAttribute("estados", licenciaService.obtenerTodosEstados());
            model.addAttribute("tipos", licenciaService.obtenerTodosTipos());
            
            return "licencias/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/licencias";
        }
    }

    // ==================== GUARDAR LICENCIA ====================
    
    @PostMapping("/guardar")
    public String guardar(LicenciaInfo licencia, RedirectAttributes flash) {
        try {
            //Delegado al service
            LicenciaInfo licenciaGuardada = licenciaService.crear(licencia);
            
            flash.addFlashAttribute("success", 
                "Licencia para '" + licenciaGuardada.getSoftwareInfo().getSftNom() + 
                "' creada exitosamente");
            
            return "redirect:/licencias/" + licenciaGuardada.getLicenciaId();
            
        } catch (LicenciaInfoService.LicenciaInvalidaException |
                 LicenciaInfoService.EstadoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/licencias/nuevo";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/licencias/nuevo";
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            LicenciaInfo licencia = licenciaService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Licencia no encontrada"));
            
            model.addAttribute("licencia", licencia);
            model.addAttribute("software", licenciaService.obtenerTodosSoftware());
            model.addAttribute("estados", licenciaService.obtenerTodosEstados());
            model.addAttribute("tipos", licenciaService.obtenerTodosTipos());
            
            return "licencias/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/licencias";
        }
    }

    // ==================== ACTUALIZAR LICENCIA ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id, 
            LicenciaInfo licencia, 
            RedirectAttributes flash) {
        
        try {
            //Delegado al service
            LicenciaInfo licenciaActualizada = licenciaService.actualizar(id, licencia);
            
            flash.addFlashAttribute("success", "Licencia actualizada exitosamente");
            
            return "redirect:/licencias/" + licenciaActualizada.getLicenciaId();
            
        } catch (LicenciaInfoService.LicenciaNoEncontradaException | 
                 LicenciaInfoService.LicenciaInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/licencias/editar/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/licencias/editar/" + id;
        }
    }

    // ==================== ELIMINAR LICENCIA ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id, 
            Principal principal, 
            RedirectAttributes flash) {
        
        try {
            //Validar que sea administrador
            validarEsAdministrador(principal);
            
            //Delegado al service (valida que no esté en uso)
            licenciaService.eliminar(id);
            
            flash.addFlashAttribute("success", "Licencia eliminada exitosamente");
            return "redirect:/licencias";
            
        } catch (LicenciaInfoService.LicenciaEnUsoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/licencias/" + id;
        } catch (LicenciaInfoService.LicenciaNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/licencias";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/licencias/" + id;
        }
    }

    // ==================== GESTIÓN DE CUPOS ====================
    
    @PostMapping("/{id}/incrementar-uso")
    public String incrementarUso(
            @PathVariable Long id,
            RedirectAttributes flash) {
        
        try {
            //Delegado al service (valida cupos y vencimiento)
            licenciaService.incrementarUso(id);
            
            flash.addFlashAttribute("success", "Uso de licencia incrementado exitosamente");
            return "redirect:/licencias/" + id;
            
        } catch (LicenciaInfoService.SinCuposDisponiblesException |
                 LicenciaInfoService.LicenciaVencidaException |
                 LicenciaInfoService.LicenciaNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/licencias/" + id;
        }
    }

    @PostMapping("/{id}/decrementar-uso")
    public String decrementarUso(
            @PathVariable Long id,
            RedirectAttributes flash) {
        
        try {
            //Delegado al service (valida que haya usos)
            licenciaService.decrementarUso(id);
            
            flash.addFlashAttribute("success", "Uso de licencia decrementado exitosamente");
            return "redirect:/licencias/" + id;
            
        } catch (LicenciaInfoService.UsoInvalidoException |
                 LicenciaInfoService.LicenciaNoEncontradaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/licencias/" + id;
        }
    }

    // ==================== FILTROS RÁPIDOS ====================
    
    @GetMapping("/vencidas")
    public String listarVencidas(Model model) {
        return "redirect:/licencias?filtro=vencidas";
    }

    @GetMapping("/proximas-vencer")
    public String listarProximasVencer(Model model) {
        return "redirect:/licencias?filtro=proximas-vencer";
    }

    @GetMapping("/activas")
    public String listarActivas(Model model) {
        return "redirect:/licencias?filtro=activas";
    }

    @GetMapping("/sin-cupos")
    public String listarSinCupos(Model model) {
        return "redirect:/licencias?filtro=sin-cupos";
    }

    @GetMapping("/con-cupos")
    public String listarConCupos(Model model) {
        return "redirect:/licencias?filtro=con-cupos";
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================
    
    @GetMapping("/por-software/{softwareId}")
    public String listarPorSoftware(@PathVariable Long softwareId, Model model) {
        return "redirect:/licencias?softwareId=" + softwareId;
    }

    @GetMapping("/por-tipo/{tipoId}")
    public String listarPorTipo(@PathVariable Long tipoId, Model model) {
        try {
            model.addAttribute("licencias", licenciaService.buscarPorTipo(tipoId));
            model.addAttribute("filtroActivo", "tipo");
            
            return "licencias/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/licencias";
        }
    }

    // ==================== ESTADÍSTICAS ====================
    
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        try {
            model.addAttribute("totalLicencias", licenciaService.obtenerTodas().size());
            model.addAttribute("porEstado", licenciaService.contarPorEstado());
            model.addAttribute("costoTotal", licenciaService.calcularCostoTotal());
            model.addAttribute("cuposDisponibles", licenciaService.contarCuposDisponibles());
            model.addAttribute("vencidas", licenciaService.contarVencidas());
            model.addAttribute("proximasVencer", licenciaService.contarProximasAVencer());
            
            // Listas para alertas
            model.addAttribute("licenciasVencidas", licenciaService.buscarVencidas());
            model.addAttribute("licenciasProximasVencer", licenciaService.buscarProximasAVencer());
            model.addAttribute("licenciasSinCupos", licenciaService.buscarSinCupos());
            
            return "licencias/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/licencias";
        }
    }

    // ==================== ALERTAS ====================
    
    @GetMapping("/alertas")
    public String alertas(Model model) {
        try {
            model.addAttribute("vencidas", licenciaService.buscarVencidas());
            model.addAttribute("proximasVencer", licenciaService.buscarProximasAVencer());
            model.addAttribute("sinCupos", licenciaService.buscarSinCupos());
            
            return "licencias/alertas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/licencias";
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