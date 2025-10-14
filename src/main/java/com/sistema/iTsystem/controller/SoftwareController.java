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

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.SoftwareInfo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.SoftwareInfoService;

@Controller
@RequestMapping("/software")
public class SoftwareController {

    // ==================== SERVICES ====================
    
    @Autowired
    private SoftwareInfoService softwareService;
    
    @Autowired
    private ActivoService activoService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR SOFTWARE ====================
    
    @GetMapping
    public String listar(Model model) {
        try {
            // ✅ Obtener todos los software
            model.addAttribute("softwareList", softwareService.obtenerTodos());
            
            // Estadísticas básicas
            model.addAttribute("totalSoftware", softwareService.obtenerTodos().size());
            
            return "software/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar software: " + e.getMessage());
            return "software/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            // ✅ Obtener software con todas las relaciones
            SoftwareInfo software = softwareService.buscarPorIdConDetalles(id)
                .orElseThrow(() -> new RuntimeException("Software no encontrado"));
            
            model.addAttribute("software", software);
            model.addAttribute("activo", software.getActivo());
            
            // ✅ Verificar si tiene licencias asociadas
            model.addAttribute("tieneLicencias", 
                softwareService.tieneLicenciasAsociadas(software));
            
            // ✅ Verificar si tiene licencias activas
            model.addAttribute("tieneLicenciasActivas", 
                softwareService.tieneLicenciasActivas(id));
            
            return "software/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
        }
    }

    // ==================== FORMULARIO NUEVO ====================
    
    @GetMapping("/nuevo")
    public String nuevoForm(
            @RequestParam(required = false) Long activoId,
            Model model) {
        
        try {
            SoftwareInfo software = new SoftwareInfo();
            
            // Si viene desde la creación de un activo
            if (activoId != null) {
                Activo activo = activoService.buscarPorId(activoId)
                    .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
                
                // Validar que sea categoría Software
                if (!activo.getCategoria().getCatNom().toLowerCase().contains("software")) {
                    throw new RuntimeException("El activo no es de categoría Software");
                }
                
                software.setActivo(activo);
                model.addAttribute("activoSeleccionado", activo);
            }
            
            model.addAttribute("software", software);
            
            // ✅ Catálogos que SÍ existen
            model.addAttribute("tipos", softwareService.obtenerTodosTipos());
            model.addAttribute("proveedores", softwareService.obtenerTodosProveedores());
            
            return "software/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
        }
    }

    // ==================== GUARDAR SOFTWARE ====================
    
    @PostMapping("/guardar")
    public String guardar(
            SoftwareInfo software,
            @RequestParam(required = false) Long activoId,
            RedirectAttributes flash) {
        
        try {
            // Asignar activo si viene por parámetro
            if (activoId != null && software.getActivo() == null) {
                Activo activo = activoService.buscarPorId(activoId)
                    .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
                software.setActivo(activo);
            }
            
            // ✅ Delegado al service
            SoftwareInfo softwareGuardado = softwareService.crear(software);
            
            flash.addFlashAttribute("success", 
                "Software '" + softwareGuardado.getSftNom() + "' registrado exitosamente");
            
            return "redirect:/software/" + softwareGuardado.getSftId();
            
        } catch (SoftwareInfoService.SoftwareInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/software/nuevo" + (activoId != null ? "?activoId=" + activoId : "");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/software/nuevo" + (activoId != null ? "?activoId=" + activoId : "");
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            SoftwareInfo software = softwareService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Software no encontrado"));
            
            model.addAttribute("software", software);
            model.addAttribute("tipos", softwareService.obtenerTodosTipos());
            model.addAttribute("proveedores", softwareService.obtenerTodosProveedores());
            
            return "software/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
        }
    }

    // ==================== ACTUALIZAR SOFTWARE ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id, 
            SoftwareInfo software, 
            RedirectAttributes flash) {
        
        try {
            // ✅ Delegado al service
            SoftwareInfo softwareActualizado = softwareService.actualizar(id, software);
            
            flash.addFlashAttribute("success", 
                "Software '" + softwareActualizado.getSftNom() + "' actualizado exitosamente");
            
            return "redirect:/software/" + softwareActualizado.getSftId();
            
        } catch (SoftwareInfoService.SoftwareNoEncontradoException | 
                 SoftwareInfoService.SoftwareInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/software/editar/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/software/editar/" + id;
        }
    }

    // ==================== ELIMINAR SOFTWARE ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id, 
            Principal principal, 
            RedirectAttributes flash) {
        
        try {
            // ✅ Validar que sea administrador
            validarEsAdministrador(principal);
            
            // ✅ Delegado al service (valida que no tenga licencias)
            softwareService.eliminar(id);
            
            flash.addFlashAttribute("success", "Software eliminado exitosamente");
            return "redirect:/software";
            
        } catch (SoftwareInfoService.SoftwareConDependenciasException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/software/" + id;
        } catch (SoftwareInfoService.SoftwareNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/software";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/software/" + id;
        }
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================
    
    @GetMapping("/buscar")
    public String buscarPorNombre(
            @RequestParam String nombre,
            Model model) {
        
        try {
            model.addAttribute("softwareList", softwareService.buscarPorNombre(nombre));
            model.addAttribute("busqueda", nombre);
            
            return "software/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
        }
    }

    @GetMapping("/por-tipo/{tipoId}")
    public String listarPorTipo(@PathVariable Long tipoId, Model model) {
        try {
            model.addAttribute("softwareList", softwareService.buscarPorTipo(tipoId));
            model.addAttribute("filtroActivo", "tipo");
            
            return "software/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
        }
    }

    @GetMapping("/por-proveedor/{proveedorId}")
    public String listarPorProveedor(@PathVariable Long proveedorId, Model model) {
        try {
            model.addAttribute("softwareList", softwareService.buscarPorProveedor(proveedorId));
            model.addAttribute("filtroActivo", "proveedor");
            
            return "software/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
        }
    }

    @GetMapping("/sin-proveedor")
    public String listarSinProveedor(Model model) {
        try {
            model.addAttribute("softwareList", softwareService.buscarSinProveedor());
            model.addAttribute("filtroActivo", "sin-proveedor");
            
            return "software/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
        }
    }

    // ==================== GESTIÓN DE LICENCIAS ====================
    
    @GetMapping("/{id}/licencias")
    public String verLicencias(@PathVariable Long id, Model model) {
        try {
            SoftwareInfo software = softwareService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Software no encontrado"));
            
            model.addAttribute("software", software);
            model.addAttribute("tieneLicencias", softwareService.tieneLicenciasAsociadas(software));
            model.addAttribute("tieneLicenciasActivas", softwareService.tieneLicenciasActivas(id));
            
            // Redireccionar al módulo de licencias con filtro por software
            return "redirect:/licencias?softwareId=" + id;
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software/" + id;
        }
    }

    // ==================== ESTADÍSTICAS ====================
    
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        try {
            model.addAttribute("totalSoftware", softwareService.obtenerTodos().size());
            model.addAttribute("porTipo", softwareService.contarPorTipo());
            model.addAttribute("porProveedor", softwareService.contarPorProveedor());
            
            return "software/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/software";
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