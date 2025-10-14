package com.sistema.iTsystem.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.EstadoTransicionService;
import com.sistema.iTsystem.service.HardwareInfoService;
import com.sistema.iTsystem.service.SoftwareInfoService;

@Controller
@RequestMapping("/activos")
public class ActivoController {

    // ==================== SERVICES ====================
    
    @Autowired
    private ActivoService activoService;
    
    @Autowired
    private HardwareInfoService hardwareService;
    
    @Autowired
    private SoftwareInfoService softwareService;
    
    @Autowired
    private EstadoTransicionService estadoTransicionService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== LISTAR ACTIVOS ====================
    
    @GetMapping
    public String listarActivos(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "categoria", required = false) Long categoria,
            @RequestParam(value = "estado", required = false) Long estado,
            @RequestParam(value = "departamento", required = false) Long departamento,
            Model model) {

        try {
            Pageable pageable = PageRequest.of(pagina, 10, Sort.by("activoFechaIngreso").descending());
            
            // ✅ Delegado al service con filtros
            Page<Activo> paginaActivos = activoService.buscarConFiltros(
                buscar, categoria, estado, departamento, pageable
            );

            model.addAttribute("activos", paginaActivos.getContent());
            model.addAttribute("paginaActual", pagina);
            model.addAttribute("totalPaginas", paginaActivos.getTotalPages());
            model.addAttribute("totalElementos", paginaActivos.getTotalElements());
            
            // Catálogos para filtros
            model.addAttribute("categorias", activoService.obtenerTodasCategorias());
            model.addAttribute("estados", activoService.obtenerTodosEstados());
            model.addAttribute("departamentos", activoService.obtenerTodosDepartamentos());
            
            // Mantener valores de filtros
            model.addAttribute("buscar", buscar);
            model.addAttribute("categoria", categoria);
            model.addAttribute("estado", estado);
            model.addAttribute("departamento", departamento);

            return "activos/listar";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar los activos: " + e.getMessage());
            return "activos/listar";
        }
    }

    // ==================== VER DETALLE ====================
    
    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            // Obtener activo
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
            
            model.addAttribute("activo", activo);
            
            // ✅ Verificar si es Hardware o Software según categoría
            String tipoCategoria = activo.getCategoria().getCatNom().toLowerCase();
            
            if (tipoCategoria.contains("hardware")) {
                // Obtener info de hardware (con proveedor, modelo, garantía)
                hardwareService.buscarPorActivoIdConDetalles(id).ifPresent(hw -> {
                    model.addAttribute("hardwareInfo", hw);
                    model.addAttribute("tipoActivo", "hardware");
                });
            } else if (tipoCategoria.contains("software")) {
                // Obtener info de software (con proveedor, tipo, licencias)
                softwareService.buscarPorActivoId(id).ifPresent(sw -> {
                    model.addAttribute("softwareInfo", sw);
                    model.addAttribute("tipoActivo", "software");
                });
            }
            
            // ✅ Obtener historial de estados (FSM)
            model.addAttribute("historialEstados", 
                estadoTransicionService.obtenerHistorialEstados(id));
            
            // ✅ Obtener estados posibles para cambio rápido
            model.addAttribute("estadosPosibles", 
                estadoTransicionService.obtenerEstadosPosibles(id));
            
            return "activos/detalle";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos";
        }
    }

    // ==================== FORMULARIO NUEVO ====================
    
    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        try {
            model.addAttribute("activo", new Activo());
            
            // Catálogos básicos
            model.addAttribute("categorias", activoService.obtenerTodasCategorias());
            model.addAttribute("departamentos", activoService.obtenerTodosDepartamentos());
            
            return "activos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
            return "error";
        }
    }

    // ==================== GUARDAR ACTIVO ====================
    
    @PostMapping("/guardar")
    public String guardar(Activo activo, RedirectAttributes flash) {
        try {
            // ✅ TODO delegado al service (validaciones, estado default, etc.)
            Activo activoGuardado = activoService.crear(activo);
            
            flash.addFlashAttribute("success", 
                "Activo '" + activoGuardado.getActivoNom() + "' creado exitosamente");
            
            // Redireccionar según tipo para completar datos
            String tipoCategoria = activoGuardado.getCategoria().getCatNom().toLowerCase();
            
            if (tipoCategoria.contains("hardware")) {
                return "redirect:/hardware/nuevo?activoId=" + activoGuardado.getActivoId();
            } else if (tipoCategoria.contains("software")) {
                return "redirect:/software/nuevo?activoId=" + activoGuardado.getActivoId();
            }
            
            return "redirect:/activos/" + activoGuardado.getActivoId();
            
        } catch (ActivoService.ActivoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/nuevo";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/activos/nuevo";
        }
    }

    // ==================== FORMULARIO EDITAR ====================
    
    @GetMapping("/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
            
            model.addAttribute("activo", activo);
            model.addAttribute("categorias", activoService.obtenerTodasCategorias());
            model.addAttribute("departamentos", activoService.obtenerTodosDepartamentos());
            
            //NO permitir cambiar estado aquí (usar cambiar-estado)
            
            return "activos/formulario";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos";
        }
    }

    // ==================== ACTUALIZAR ACTIVO ====================
    
    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id, Activo activo, RedirectAttributes flash) {
        try {
            //Delegado al service (NO cambia estado aquí)
            Activo activoActualizado = activoService.actualizar(id, activo);
            
            flash.addFlashAttribute("success", 
                "Activo '" + activoActualizado.getActivoNom() + "' actualizado exitosamente");
            
            return "redirect:/activos/" + id;
            
        } catch (ActivoService.ActivoNoEncontradoException | 
                 ActivoService.ActivoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/editar/" + id;
        }
    }

    // ==================== CAMBIAR ESTADO (FSM) ====================
    
    @GetMapping("/{id}/cambiar-estado")
    public String cambiarEstadoForm(@PathVariable Long id, Model model, Principal principal) {
        try {
            //Validar permisos (solo admin para ciertos cambios)
            validarPermisos(principal);
            
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
            
            model.addAttribute("activo", activo);
            
            //Estados posibles según FSM y categoría
            model.addAttribute("estadosPosibles", 
                estadoTransicionService.obtenerEstadosPosibles(id));
            
            return "activos/cambiar-estado";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    @PostMapping("/{id}/cambiar-estado")
    public String cambiarEstado(
            @PathVariable Long id,
            @RequestParam Long nuevoEstadoId,
            @RequestParam String motivo,
            @RequestParam(required = false) String observaciones,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            //Obtener usuario actual
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            //Delegado al service (valida FSM automáticamente)
            activoService.cambiarEstado(id, nuevoEstadoId, motivo, observaciones, usuario);
            
            flash.addFlashAttribute("success", "Estado cambiado exitosamente");
            return "redirect:/activos/" + id;
            
        } catch (EstadoTransicionService.TransicionInvalidaException e) {
            flash.addFlashAttribute("error", "Transición inválida: " + e.getMessage());
            return "redirect:/activos/" + id + "/cambiar-estado";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    // ==================== DAR DE BAJA (SOLO ADMIN) ====================
    
    @PostMapping("/{id}/dar-baja")
    public String darBaja(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam(required = false) String observaciones,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            //Solo admin puede dar de baja directa
            Usuario usuario = validarEsAdministrador(principal);
            
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
            
            //Obtener estado "Baja" o "Cancelado" según categoría
            String estadoFinal = obtenerEstadoFinal(activo);
            EstadoActivo estadoBaja = activoService.obtenerTodosEstados().stream()
                .filter(e -> estadoFinal.equals(e.getEstadoNom()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Estado '" + estadoFinal + "' no encontrado"));
            
            //Validar que no esté ya dado de baja
            if (estadoFinal.equals(activo.getEstado().getEstadoNom())) {
                throw new RuntimeException("El activo ya está en estado '" + estadoFinal + "'");
            }
            
            //Cambiar estado directamente
            activoService.cambiarEstado(id, estadoBaja.getEstadoId(), motivo, observaciones, usuario);
            
            flash.addFlashAttribute("success", 
                "Activo dado de baja exitosamente. Esta acción es IRREVERSIBLE.");
            
            return "redirect:/activos/" + id;
            
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    // ==================== SOLICITAR BAJA (USUARIOS NORMALES) ====================
    
    @PostMapping("/{id}/solicitar-baja")
    public String solicitarBaja(
            @PathVariable Long id,
            @RequestParam String motivo,
            Principal principal,
            RedirectAttributes flash) {
        
        try {
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
            
            //Obtener estado "Pendiente de Baja"
            EstadoActivo estadoPendienteBaja = activoService.obtenerTodosEstados().stream()
                .filter(e -> "Pendiente de Baja".equals(e.getEstadoNom()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Estado 'Pendiente de Baja' no encontrado"));
            
            //Cambiar a "Pendiente de Baja"
            activoService.cambiarEstado(id, estadoPendienteBaja.getEstadoId(), 
                motivo, "Solicitud de baja por usuario", usuario);
            
            flash.addFlashAttribute("success", 
                "Solicitud de baja enviada. El administrador debe aprobarla.");
            
            return "redirect:/activos/" + id;
            
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    // ==================== ELIMINAR ACTIVO ====================
    
    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            //Solo admin puede eliminar
            validarEsAdministrador(principal);
            
            //Delegado al service (valida que esté en estado "Baja")
            activoService.eliminar(id);
            
            flash.addFlashAttribute("success", "Activo eliminado exitosamente");
            return "redirect:/activos";
            
        } catch (ActivoService.ActivoConDependenciasException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    // ==================== VER HISTORIAL DE ESTADOS (FSM) ====================
    
    @GetMapping("/{id}/historial")
    public String verHistorial(@PathVariable Long id, Model model) {
        try {
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));
            
            //Historial completo de cambios de estado
            model.addAttribute("activo", activo);
            model.addAttribute("historial", 
                estadoTransicionService.obtenerHistorialEstados(id));
            
            return "activos/historial";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    // ==================== ENDPOINT AJAX: ESTADOS POR CATEGORÍA ====================
    
    @GetMapping("/estados-por-categoria/{categoriaId}")
    @ResponseBody
    public java.util.List<EstadoActivo> obtenerEstadosPorCategoria(@PathVariable Long categoriaId) {
        return activoService.obtenerEstadosPorCategoria(categoriaId);
    }

    // ==================== MÉTODOS HELPER ====================
    
    /**
     * Validar que el usuario tenga permisos
     */
    private void validarPermisos(Principal principal) {
        usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
    
    /**
     * Validar que el usuario sea administrador (rol_id = 1)
     */
    private Usuario validarEsAdministrador(Principal principal) {
        Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!usuario.getRol().getRolId().equals(1L)) {
            throw new RuntimeException("Esta acción requiere privilegios de Administrador");
        }
        
        return usuario;
    }
    
    /**
     * Obtener estado final según categoría (Baja o Cancelado)
     */
    private String obtenerEstadoFinal(Activo activo) {
        String nombreCategoria = activo.getCategoria().getCatNom().toLowerCase();
        
        if (nombreCategoria.contains("servicio") || nombreCategoria.contains("contrato")) {
            return "Cancelado";
        }
        
        return "Baja";
    }
}