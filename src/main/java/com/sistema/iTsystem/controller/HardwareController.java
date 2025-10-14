package com.sistema.iTsystem.controller;

import java.math.BigDecimal;
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
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.HardwareInfoService;

@Controller
@RequestMapping("/hardware")
public class HardwareController {

    // ==================== SERVICES ====================
    
    @Autowired
    private HardwareInfoService hardwareService;
    
    @Autowired
    private ActivoService activoService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR HARDWARE ====================
    
    @GetMapping
    public String listar(Model model) {
        try {
            //Obtener todos los hardware
            model.addAttribute("hardwareList", hardwareService.obtenerTodos());
            
            // Estadísticas básicas
            model.addAttribute("totalHardware", hardwareService.obtenerTodos().size());
            model.addAttribute("valorTotal", hardwareService.calcularValorTotal());
            model.addAttribute("valorPromedio", hardwareService.calcularValorPromedio());
            
            return "hardware/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar hardware: " + e.getMessage());
            return "hardware/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            //Obtener hardware con todas las relaciones
            HardwareInfo hardware = hardwareService.buscarPorIdConDetalles(id)
                .orElseThrow(() -> new RuntimeException("Hardware no encontrado"));
            
            model.addAttribute("hardware", hardware);
            model.addAttribute("activo", hardware.getActivo());
            
            //Verificar si tiene garantía vigente
            model.addAttribute("tieneGarantiaVigente", 
                hardwareService.tieneGarantiaVigente(hardware));
            
            return "hardware/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    // ==================== FORMULARIO NUEVO ====================
    
    @GetMapping("/nuevo")
    public String nuevoForm(
            @RequestParam(required = false) Long activoId,
            Model model) {
        
        try {
            HardwareInfo hardware = new HardwareInfo();
            
            // Si viene desde la creación de un activo
            if (activoId != null) {
                Activo activo = activoService.buscarPorId(activoId)
                    .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
                
                // Validar que sea categoría Hardware
                if (!activo.getCategoria().getCatNom().toLowerCase().contains("hardware")) {
                    throw new RuntimeException("El activo no es de categoría Hardware");
                }
                
                hardware.setActivo(activo);
                model.addAttribute("activoSeleccionado", activo);
            }
            
            model.addAttribute("hardware", hardware);
            
            //Catálogos que SÍ existen
            model.addAttribute("modelos", hardwareService.obtenerTodosModelos());
            model.addAttribute("proveedores", hardwareService.obtenerTodosProveedores());
            
            // Si no viene activoId, mostrar lista de activos disponibles
            // (Aquí podrías filtrar activos de hardware sin hardware_info asociado)
            
            return "hardware/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    // ==================== GUARDAR HARDWARE ====================
    
    @PostMapping("/guardar")
    public String guardar(
            HardwareInfo hardware,
            @RequestParam(required = false) Long activoId,
            RedirectAttributes flash) {
        
        try {
            // Asignar activo si viene por parámetro
            if (activoId != null && hardware.getActivo() == null) {
                Activo activo = activoService.buscarPorId(activoId)
                    .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
                hardware.setActivo(activo);
            }
            
            //Delegado al service
            HardwareInfo hardwareGuardado = hardwareService.crear(hardware);
            
            flash.addFlashAttribute("success", 
                "Hardware registrado exitosamente para el activo: " + 
                hardwareGuardado.getActivo().getActivoNom());
            
            return "redirect:/hardware/" + hardwareGuardado.getHwId();
            
        } catch (HardwareInfoService.HardwareInvalidoException | 
                 HardwareInfoService.SerialDuplicadoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/hardware/nuevo" + (activoId != null ? "?activoId=" + activoId : "");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/hardware/nuevo" + (activoId != null ? "?activoId=" + activoId : "");
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            HardwareInfo hardware = hardwareService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Hardware no encontrado"));
            
            model.addAttribute("hardware", hardware);
            model.addAttribute("modelos", hardwareService.obtenerTodosModelos());
            model.addAttribute("proveedores", hardwareService.obtenerTodosProveedores());
            
            return "hardware/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    // ==================== ACTUALIZAR HARDWARE ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id, 
            HardwareInfo hardware, 
            RedirectAttributes flash) {
        
        try {
            //Delegado al service
            HardwareInfo hardwareActualizado = hardwareService.actualizar(id, hardware);
            
            flash.addFlashAttribute("success", "Hardware actualizado exitosamente");
            
            return "redirect:/hardware/" + hardwareActualizado.getHwId();
            
        } catch (HardwareInfoService.HardwareNoEncontradoException | 
                 HardwareInfoService.HardwareInvalidoException |
                 HardwareInfoService.SerialDuplicadoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/hardware/editar/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/hardware/editar/" + id;
        }
    }

    // ==================== ELIMINAR HARDWARE ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id, 
            Principal principal, 
            RedirectAttributes flash) {
        
        try {
            // ✅ Validar que sea administrador
            validarEsAdministrador(principal);
            
            // ✅ Delegado al service
            hardwareService.eliminar(id);
            
            flash.addFlashAttribute("success", "Hardware eliminado exitosamente");
            return "redirect:/hardware";
            
        } catch (HardwareInfoService.HardwareNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/hardware/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/hardware/" + id;
        }
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================
    
    @GetMapping("/por-serial")
    public String buscarPorSerial(
            @RequestParam String serial,
            Model model,
            RedirectAttributes flash) {
        
        try {
            HardwareInfo hardware = hardwareService.buscarPorSerial(serial)
                .orElseThrow(() -> new RuntimeException("Hardware con serial '" + serial + "' no encontrado"));
            
            return "redirect:/hardware/" + hardware.getHwId();
            
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    @GetMapping("/por-modelo/{modeloId}")
    public String listarPorModelo(@PathVariable Long modeloId, Model model) {
        try {
            model.addAttribute("hardwareList", hardwareService.buscarPorModelo(modeloId));
            model.addAttribute("filtroActivo", "modelo");
            
            return "hardware/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    @GetMapping("/por-proveedor/{proveedorId}")
    public String listarPorProveedor(@PathVariable Long proveedorId, Model model) {
        try {
            model.addAttribute("hardwareList", hardwareService.buscarPorProveedor(proveedorId));
            model.addAttribute("filtroActivo", "proveedor");
            
            return "hardware/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    @GetMapping("/sin-proveedor")
    public String listarSinProveedor(Model model) {
        try {
            model.addAttribute("hardwareList", hardwareService.buscarSinProveedor());
            model.addAttribute("filtroActivo", "sin-proveedor");
            
            return "hardware/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    @GetMapping("/sin-valor-compra")
    public String listarSinValorCompra(Model model) {
        try {
            model.addAttribute("hardwareList", hardwareService.buscarSinValorCompra());
            model.addAttribute("filtroActivo", "sin-valor");
            
            return "hardware/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    @GetMapping("/por-valor")
    public String listarPorValor(
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max,
            Model model) {
        
        try {
            if (min != null && max != null) {
                model.addAttribute("hardwareList", hardwareService.buscarPorValorEntre(min, max));
            } else if (min != null) {
                model.addAttribute("hardwareList", hardwareService.buscarPorValorMayorA(min));
            } else {
                model.addAttribute("hardwareList", hardwareService.obtenerTodos());
            }
            
            return "hardware/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
        }
    }

    // ==================== ESTADÍSTICAS ====================
    
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        try {
            model.addAttribute("totalHardware", hardwareService.obtenerTodos().size());
            model.addAttribute("valorTotal", hardwareService.calcularValorTotal());
            model.addAttribute("valorPromedio", hardwareService.calcularValorPromedio());
            model.addAttribute("porProveedor", hardwareService.contarPorProveedor());
            model.addAttribute("porModelo", hardwareService.contarPorModelo());
            model.addAttribute("sinValorCompra", hardwareService.contarSinValorCompra());
            
            return "hardware/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/hardware";
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