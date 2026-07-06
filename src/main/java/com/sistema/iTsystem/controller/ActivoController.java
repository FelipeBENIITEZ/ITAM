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
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.EstadoTransicionService;
import com.sistema.iTsystem.service.HardwareInfoService;

@Controller
@RequestMapping("/activos")
public class ActivoController {

    @Autowired
    private ActivoService activoService;

    @Autowired
    private HardwareInfoService hardwareService;

    @Autowired
    private EstadoTransicionService estadoTransicionService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriasActivoRepository categoriasRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

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
            Page<Activo> paginaActivos = activoService.buscarConFiltros(
                buscar, categoria, estado, departamento, pageable
            );

            model.addAttribute("activos", paginaActivos.getContent());
            model.addAttribute("paginaActual", pagina);
            model.addAttribute("totalPaginas", paginaActivos.getTotalPages());
            model.addAttribute("totalElementos", paginaActivos.getTotalElements());
            model.addAttribute("categorias", activoService.obtenerTodasCategorias());
            model.addAttribute("estados", activoService.obtenerTodosEstados());
            model.addAttribute("departamentos", activoService.obtenerTodosDepartamentos());
            model.addAttribute("buscar", buscar);
            model.addAttribute("categoria", categoria);
            model.addAttribute("estado", estado);
            model.addAttribute("departamento", departamento);

            return "activos";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar los activos: " + e.getMessage());
            return "activos";
        }
    }

    @GetMapping("/{activoCodigo}")
    public String verDetalle(@PathVariable String activoCodigo, Model model) {
        try {
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);

            hardwareService.buscarPorActivoIdConDetalles(activo.getActivoId()).ifPresent(hw -> {
                model.addAttribute("hardwareInfo", hw);
                model.addAttribute("garantia", hw.getGarantia());
                model.addAttribute("tipoActivo", "hardware");
            });

            model.addAttribute("historialEstados", estadoTransicionService.obtenerHistorialEstados(activo.getActivoId()));

            return "activo-detalle";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos";
        }
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        model.addAttribute("activo", new Activo());
        model.addAttribute("categorias", activoService.obtenerTodasCategorias());
        model.addAttribute("marcas", activoService.obtenerTodasMarcas());
        model.addAttribute("modelos", activoService.obtenerTodosModelos());
        model.addAttribute("proveedores", activoService.obtenerTodosProveedores());
        model.addAttribute("modoEdicion", false);
        return "activos/nuevo";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam String activoCodigo,
            @RequestParam String activoNom,
            @RequestParam(required = false) String activoDescri,
            @RequestParam Long marcaId,
            @RequestParam Long modeloId,
            @RequestParam String numeroSerie,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam Long categoriaId,
            Principal principal,
            RedirectAttributes flash) {
        try {
            Usuario usuarioOperador = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            Activo activo = new Activo();
            activo.setActivoCodigo(activoCodigo);
            activo.setActivoNom(activoNom);
            activo.setActivoDescri(activoDescri);
            activo.setCategoria(categoriasRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoria no encontrada")));

            if (proveedorId != null) {
                activo.setProveedor(proveedoresRepository.findById(proveedorId)
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado")));
            }

            Activo activoGuardado = activoService.crear(activo, marcaId, modeloId, numeroSerie);
            estadoTransicionService.registrarCreacion(
                activoGuardado,
                usuarioOperador,
                "Creacion de activo",
                activoDescri
            );

            flash.addFlashAttribute("success",
                "Activo '" + activoGuardado.getActivoNom() + "' creado exitosamente");

            return "redirect:/activos/" + activoGuardado.getActivoCodigo();
        } catch (ActivoService.ActivoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/nuevo";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/activos/nuevo";
        }
    }

    @GetMapping("/{activoCodigo}/editar")
    public String editarForm(@PathVariable String activoCodigo, Model model) {
        try {
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("categorias", activoService.obtenerTodasCategorias());
            model.addAttribute("hardwareInfo",
                hardwareService.buscarPorActivoIdConDetalles(activo.getActivoId()).orElse(null));
            model.addAttribute("proveedores", activoService.obtenerTodosProveedores());
            model.addAttribute("modoEdicion", true);

            return "activos/nuevo";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos";
        }
    }

    @PostMapping("/{activoCodigo}/editar")
    public String actualizar(
            @PathVariable String activoCodigo,
            @RequestParam String activoNom,
            @RequestParam(required = false) String activoDescri,
            @RequestParam(required = false) Long proveedorId,
            RedirectAttributes flash) {
        try {
            Activo activoExistente = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            Activo activo = new Activo();
            activo.setActivoNom(activoNom);
            activo.setActivoDescri(activoDescri);

            if (proveedorId != null) {
                activo.setProveedor(proveedoresRepository.findById(proveedorId)
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado")));
            }

            Activo activoActualizado = activoService.actualizar(activoExistente.getActivoId(), activo);
            flash.addFlashAttribute("success",
                "Activo '" + activoActualizado.getActivoNom() + "' actualizado exitosamente");
            return "redirect:/activos/" + activoActualizado.getActivoCodigo();
        } catch (ActivoService.ActivoNoEncontradoException | ActivoService.ActivoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo + "/editar";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo + "/editar";
        }
    }

    @GetMapping("/{activoCodigo}/operacion")
    public String operacionForm(
            @PathVariable String activoCodigo,
            @RequestParam String operacion,
            Model model,
            Principal principal) {
        try {
            validarPermisos(principal);
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("operacion", operacion);
            cargarDatosOperacion(model, activo, operacion);

            return "activos/cambiar-estado";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo;
        }
    }

    @PostMapping("/{activoCodigo}/operacion")
    public String ejecutarOperacion(
            @PathVariable String activoCodigo,
            @RequestParam String operacion,
            @RequestParam String motivo,
            @RequestParam(required = false) String observaciones,
            Principal principal,
            RedirectAttributes flash) {
        try {
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            String operacionNormalizada = operacion != null ? operacion.trim().toLowerCase() : "";

            if ("baja".equals(operacionNormalizada)) {
                usuario = validarEsAdministrador(principal);
            }

            ejecutarOperacion(activoCodigo, operacionNormalizada, motivo, observaciones, usuario);
            flash.addFlashAttribute("success", "Operacion registrada correctamente");
            return "redirect:/activos/" + activoCodigo;
        } catch (EstadoTransicionService.TransicionInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo + "/operacion?operacion=" + operacion;
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo;
        }
    }

    @PostMapping("/{activoCodigo}/dar-baja")
    public String darBaja(
            @PathVariable String activoCodigo,
            @RequestParam String motivo,
            @RequestParam(required = false) String observaciones,
            Principal principal,
            RedirectAttributes flash) {
        try {
            Usuario usuario = validarEsAdministrador(principal);
            estadoTransicionService.darDeBaja(activoCodigo, motivo, observaciones, usuario);
            flash.addFlashAttribute("success", "Activo dado de baja exitosamente");
            return "redirect:/activos/" + activoCodigo;
        } catch (EstadoTransicionService.TransicionInvalidaException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo + "/operacion?operacion=baja";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo;
        }
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, Principal principal, RedirectAttributes flash) {
        try {
            validarEsAdministrador(principal);
            activoService.eliminar(id);
            flash.addFlashAttribute("success", "Activo eliminado exitosamente");
            return "redirect:/activos";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    @GetMapping("/{activoCodigo}/historial")
    public String verHistorial(@PathVariable String activoCodigo, Model model) {
        try {
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("historial", estadoTransicionService.obtenerHistorialEstados(activo.getActivoId()));

            return "activos/historial";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo;
        }
    }

    @GetMapping("/estados-por-categoria/{categoriaId}")
    @ResponseBody
    public java.util.List<EstadoActivo> obtenerEstadosPorCategoria(@PathVariable Long categoriaId) {
        return activoService.obtenerEstadosPorCategoria(categoriaId);
    }

    private void validarPermisos(Principal principal) {
        usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private Usuario validarEsAdministrador(Principal principal) {
        Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!usuario.esAdministrador()) {
            throw new RuntimeException("Esta accion requiere privilegios de Administrador");
        }

        return usuario;
    }

    private void cargarDatosOperacion(Model model, Activo activo, String operacion) {
        String operacionNormalizada = operacion != null ? operacion.trim().toLowerCase() : "";
        String titulo;
        String estadoResultante;
        String descripcion;

        switch (operacionNormalizada) {
            case "mantenimiento" -> {
                titulo = "Enviar a mantenimiento";
                estadoResultante = "En mantenimiento";
                descripcion = "El activo pasará a mantenimiento y quedará fuera de uso operativo.";
            }
            case "finalizar-mantenimiento" -> {
                titulo = "Finalizar mantenimiento";
                estadoResultante = "Disponible";
                descripcion = "El activo volverá a estar disponible para uso operativo.";
            }
            case "baja" -> {
                titulo = "Dar de baja";
                estadoResultante = "Dado de baja";
                descripcion = "La baja es lógica. El activo seguirá visible con su historial.";
            }
            default -> throw new RuntimeException("Operacion no reconocida");
        }

        model.addAttribute("operacion", operacionNormalizada);
        model.addAttribute("tituloOperacion", titulo);
        model.addAttribute("estadoResultante", estadoResultante);
        model.addAttribute("descripcionOperacion", descripcion);
        model.addAttribute("estadoActual", activo.getEstado() != null ? activo.getEstado().getEstadoNom() : "Sin estado");
    }

    private void ejecutarOperacion(String activoCodigo, String operacion, String motivo, String observaciones, Usuario usuario) {
        String operacionNormalizada = operacion != null ? operacion.trim().toLowerCase() : "";

        switch (operacionNormalizada) {
            case "mantenimiento" -> estadoTransicionService.enviarAMantenimiento(activoCodigo, motivo, observaciones, usuario);
            case "finalizar-mantenimiento" -> estadoTransicionService.finalizarMantenimiento(activoCodigo, motivo, observaciones, usuario);
            case "baja" -> estadoTransicionService.darDeBaja(activoCodigo, motivo, observaciones, usuario);
            default -> throw new EstadoTransicionService.TransicionInvalidaException("Operacion no reconocida");
        }
    }
}
