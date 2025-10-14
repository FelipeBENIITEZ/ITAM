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

import com.sistema.iTsystem.model.ContratoInfo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ContratoInfoService;

@Controller
@RequestMapping("/contratos")
public class ContratoController {

    // ==================== SERVICES ====================
    
    @Autowired
    private ContratoInfoService contratoService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR CONTRATOS ====================
    
    @GetMapping
    public String listar(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) String filtro,
            @RequestParam(required = false) String buscar,
            Model model) {
        
        try {
            java.util.List<ContratoInfo> contratos;
            
            //Filtros
            if (proveedorId != null) {
                contratos = contratoService.buscarPorProveedor(proveedorId);
                model.addAttribute("filtroActivo", "proveedor");
            } else if (buscar != null && !buscar.trim().isEmpty()) {
                contratos = contratoService.buscarPorDescripcion(buscar);
                model.addAttribute("filtroActivo", "busqueda");
                model.addAttribute("buscar", buscar);
            } else if ("vigentes".equals(filtro)) {
                contratos = contratoService.buscarVigentes();
                model.addAttribute("filtroActivo", "vigentes");
            } else if ("vencidos".equals(filtro)) {
                contratos = contratoService.buscarVencidos();
                model.addAttribute("filtroActivo", "vencidos");
            } else if ("proximos-vencer".equals(filtro)) {
                contratos = contratoService.buscarProximosAVencer();
                model.addAttribute("filtroActivo", "proximos-vencer");
            } else if ("sin-archivo".equals(filtro)) {
                contratos = contratoService.buscarSinArchivo();
                model.addAttribute("filtroActivo", "sin-archivo");
            } else if ("con-archivo".equals(filtro)) {
                contratos = contratoService.buscarConArchivo();
                model.addAttribute("filtroActivo", "con-archivo");
            } else {
                contratos = contratoService.obtenerTodosOrdenados();
            }
            
            model.addAttribute("contratos", contratos);
            
            // Catálogos para filtros
            model.addAttribute("proveedores", contratoService.obtenerTodosProveedores());
            
            // Estadísticas resumidas
            model.addAttribute("totalContratos", contratos.size());
            model.addAttribute("vigentes", contratoService.contarVigentes());
            model.addAttribute("vencidos", contratoService.contarVencidos());
            model.addAttribute("proximosVencer", contratoService.contarProximosAVencer());
            
            return "contratos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar contratos: " + e.getMessage());
            return "contratos/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            ContratoInfo contrato = contratoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
            
            model.addAttribute("contrato", contrato);
            
            //Verificar vigencia
            model.addAttribute("estaVigente", contratoService.estaVigente(id));
            
            // Calcular días hasta vencer (si tiene fecha fin)
            if (contrato.getContratFechaFin() != null) {
                java.time.LocalDate hoy = java.time.LocalDate.now();
                
                if (!contrato.getContratFechaFin().isBefore(hoy)) {
                    long diasHastaVencer = java.time.temporal.ChronoUnit.DAYS.between(
                        hoy, 
                        contrato.getContratFechaFin()
                    );
                    model.addAttribute("diasHastaVencer", diasHastaVencer);
                    model.addAttribute("vencido", false);
                } else {
                    model.addAttribute("vencido", true);
                }
            } else {
                model.addAttribute("sinFechaFin", true);
            }
            
            //Verificar si tiene archivo
            model.addAttribute("tieneArchivo", 
                contrato.getContratArchivoPath() != null && !contrato.getContratArchivoPath().isEmpty());
            
            return "contratos/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/contratos";
        }
    }

    // ==================== FORMULARIO NUEVO ====================
    
    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        try {
            model.addAttribute("contrato", new ContratoInfo());
            
            //Catálogos
            model.addAttribute("proveedores", contratoService.obtenerTodosProveedores());
            
            return "contratos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/contratos";
        }
    }

    // ==================== GUARDAR CONTRATO ====================
    
    @PostMapping("/guardar")
    public String guardar(ContratoInfo contrato, RedirectAttributes flash) {
        try {
            //Delegado al service
            ContratoInfo contratoGuardado = contratoService.crear(contrato);
            
            flash.addFlashAttribute("success", 
                "Contrato '" + contratoGuardado.getContratNumero() + "' creado exitosamente");
            
            return "redirect:/contratos/" + contratoGuardado.getContratId();
            
        } catch (ContratoInfoService.ContratoInvalidoException |
                 ContratoInfoService.NumeroContratoDuplicadoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/contratos/nuevo";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/contratos/nuevo";
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            ContratoInfo contrato = contratoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
            
            model.addAttribute("contrato", contrato);
            model.addAttribute("proveedores", contratoService.obtenerTodosProveedores());
            
            return "contratos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/contratos";
        }
    }

    // ==================== ACTUALIZAR CONTRATO ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(
            @PathVariable Long id, 
            ContratoInfo contrato, 
            RedirectAttributes flash) {
        
        try {
            //Delegado al service
            ContratoInfo contratoActualizado = contratoService.actualizar(id, contrato);
            
            flash.addFlashAttribute("success", 
                "Contrato '" + contratoActualizado.getContratNumero() + "' actualizado exitosamente");
            
            return "redirect:/contratos/" + contratoActualizado.getContratId();
            
        } catch (ContratoInfoService.ContratoNoEncontradoException | 
                 ContratoInfoService.ContratoInvalidoException |
                 ContratoInfoService.NumeroContratoDuplicadoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/contratos/editar/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al actualizar: " + e.getMessage());
            return "redirect:/contratos/editar/" + id;
        }
    }

    // ==================== ELIMINAR CONTRATO ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(
            @PathVariable Long id, 
            Principal principal, 
            RedirectAttributes flash) {
        
        try {
            //Validar que sea administrador
            validarEsAdministrador(principal);
            
            //Delegado al service (valida que no tenga activos asociados)
            contratoService.eliminar(id);
            
            flash.addFlashAttribute("success", "Contrato eliminado exitosamente");
            return "redirect:/contratos";
            
        } catch (ContratoInfoService.ContratoConDependenciasException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/contratos/" + id;
        } catch (ContratoInfoService.ContratoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/contratos";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/contratos/" + id;
        }
    }

    // ==================== GESTIÓN DE ARCHIVOS ====================
    
    @GetMapping("/{id}/archivo")
    public String gestionarArchivo(@PathVariable Long id, Model model) {
        try {
            ContratoInfo contrato = contratoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
            
            model.addAttribute("contrato", contrato);
            model.addAttribute("tieneArchivo", 
                contrato.getContratArchivoPath() != null && !contrato.getContratArchivoPath().isEmpty());
            
            return "contratos/archivo";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/contratos/" + id;
        }
    }

    @PostMapping("/{id}/archivo/asignar")
    public String asignarArchivo(
            @PathVariable Long id,
            @RequestParam String rutaArchivo,
            RedirectAttributes flash) {
        
        try {
            //Delegado al service
            contratoService.asignarArchivo(id, rutaArchivo);
            
            flash.addFlashAttribute("success", "Archivo asignado exitosamente");
            return "redirect:/contratos/" + id;
            
        } catch (ContratoInfoService.ContratoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/contratos/" + id;
        }
    }

    @PostMapping("/{id}/archivo/eliminar")
    public String eliminarArchivo(
            @PathVariable Long id,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            validarEsAdministrador(principal);
            
            //Delegado al service
            contratoService.eliminarArchivo(id);
            
            flash.addFlashAttribute("success", "Archivo eliminado exitosamente");
            return "redirect:/contratos/" + id;
            
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/contratos/" + id;
        }
    }

    // ==================== FILTROS RÁPIDOS ====================
    
    @GetMapping("/vigentes")
    public String listarVigentes(Model model) {
        return "redirect:/contratos?filtro=vigentes";
    }

    @GetMapping("/vencidos")
    public String listarVencidos(Model model) {
        return "redirect:/contratos?filtro=vencidos";
    }

    @GetMapping("/proximos-vencer")
    public String listarProximosVencer(Model model) {
        return "redirect:/contratos?filtro=proximos-vencer";
    }

    @GetMapping("/sin-archivo")
    public String listarSinArchivo(Model model) {
        return "redirect:/contratos?filtro=sin-archivo";
    }

    @GetMapping("/con-archivo")
    public String listarConArchivo(Model model) {
        return "redirect:/contratos?filtro=con-archivo";
    }

    // ==================== BÚSQUEDAS ESPECÍFICAS ====================
    
    @GetMapping("/por-proveedor/{proveedorId}")
    public String listarPorProveedor(@PathVariable Long proveedorId, Model model) {
        return "redirect:/contratos?proveedorId=" + proveedorId;
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam String texto, Model model) {
        return "redirect:/contratos?buscar=" + texto;
    }

    @GetMapping("/verificar-numero")
    public String verificarNumero(@RequestParam String numero, Model model) {
        try {
            boolean existe = contratoService.existeNumeroContrato(numero);
            model.addAttribute("existe", existe);
            
            if (existe) {
                contratoService.buscarPorNumero(numero).ifPresent(contrato -> {
                    model.addAttribute("contrato", contrato);
                });
            }
            
            return "contratos/verificar";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/contratos";
        }
    }

    // ==================== ESTADÍSTICAS ====================
    
    @GetMapping("/estadisticas")
    public String estadisticas(Model model) {
        try {
            model.addAttribute("totalContratos", contratoService.obtenerTodos().size());
            model.addAttribute("vigentes", contratoService.contarVigentes());
            model.addAttribute("vencidos", contratoService.contarVencidos());
            model.addAttribute("proximosVencer", contratoService.contarProximosAVencer());
            model.addAttribute("conArchivo", contratoService.contarConArchivo());
            model.addAttribute("sinArchivo", contratoService.contarSinArchivo());
            
            // Listas para alertas
            model.addAttribute("contratosVencidos", contratoService.buscarVencidos());
            model.addAttribute("contratosProximosVencer", contratoService.buscarProximosAVencer());
            model.addAttribute("contratosSinArchivo", contratoService.buscarSinArchivo());
            
            return "contratos/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/contratos";
        }
    }

    // ==================== ALERTAS ====================
    
    @GetMapping("/alertas")
    public String alertas(Model model) {
        try {
            model.addAttribute("vencidos", contratoService.buscarVencidos());
            model.addAttribute("proximosVencer", contratoService.buscarProximosAVencer());
            model.addAttribute("sinArchivo", contratoService.buscarSinArchivo());
            
            return "contratos/alertas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/contratos";
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