package com.sistema.iTsystem.controller;

import java.security.Principal;
import java.time.LocalDate;

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
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
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
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

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

    @GetMapping("/{id}")
    public String verDetalle(@PathVariable Long id, Model model) {
        try {
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("asignacionActiva",
                usuarioAsignacionRepository.findByActivo_ActivoIdAndAsignacionActivaTrue(id).orElse(null));
            model.addAttribute("asignaciones",
                usuarioAsignacionRepository.findByActivoIdWithUserDetails(id));

            hardwareService.buscarPorActivoIdConDetalles(id).ifPresent(hw -> {
                model.addAttribute("hardwareInfo", hw);
                model.addAttribute("garantia", hw.getGarantia());
                model.addAttribute("tipoActivo", "hardware");
            });

            model.addAttribute("historialEstados", estadoTransicionService.obtenerHistorialEstados(id));
            model.addAttribute("estadosPosibles", estadoTransicionService.obtenerEstadosPosibles(id));

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
        model.addAttribute("estados", activoService.obtenerEstadosInicialesPermitidos());
        model.addAttribute("proveedores", activoService.obtenerTodosProveedores());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        return "activos/nuevo";
    }

    @PostMapping("/guardar")
    public String guardar(
            @RequestParam String activoCodigo,
            @RequestParam String activoNom,
            @RequestParam(required = false) String activoDescri,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam Long categoriaId,
            @RequestParam(required = false) Long estadoId,
            @RequestParam(required = false) Long usuarioAsignadoId,
            @RequestParam(required = false) String asignacionMotivo,
            RedirectAttributes flash) {
        try {
            Activo activo = new Activo();
            activo.setActivoCodigo(activoCodigo);
            activo.setActivoNom(activoNom);
            activo.setActivoDescri(activoDescri);
            activo.setCategoria(categoriasRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoria no encontrada")));

            if (usuarioAsignadoId != null) {
                activo.setEstado(buscarEstadoPorNombre("Asignado"));
            } else if (estadoId != null) {
                EstadoActivo estadoInicial = estadoActivoRepository.findById(estadoId)
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
                validarEstadoInicial(estadoInicial);
                activo.setEstado(estadoInicial);
            }
            if (proveedorId != null) {
                activo.setProveedor(proveedoresRepository.findById(proveedorId)
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado")));
            }

            Activo activoGuardado = activoService.crear(activo);

            if (usuarioAsignadoId != null) {
                Usuario usuarioAsignado = usuarioRepository.findById(usuarioAsignadoId)
                    .orElseThrow(() -> new RuntimeException("Usuario asignado no encontrado"));

                UsuarioAsignacion asignacion = new UsuarioAsignacion();
                asignacion.setActivo(activoGuardado);
                asignacion.setUsuario(usuarioAsignado);
                asignacion.setAsignacionFecha(LocalDate.now());
                asignacion.setAsignacionMotivo(asignacionMotivo);
                asignacion.setAsignacionActiva(true);
                usuarioAsignacionRepository.save(asignacion);
            }

            flash.addFlashAttribute("success",
                "Activo '" + activoGuardado.getActivoNom() + "' creado exitosamente");

            return "redirect:/activos/" + activoGuardado.getActivoId();
        } catch (ActivoService.ActivoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/nuevo";
        } catch (Exception e) {
            flash.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/activos/nuevo";
        }
    }

    @GetMapping({"/editar/{id}", "/{id}/editar"})
    public String editarForm(@PathVariable Long id, Model model) {
        try {
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("categorias", activoService.obtenerTodasCategorias());
            model.addAttribute("estados", activoService.obtenerEstadosInicialesPermitidos());
            model.addAttribute("proveedores", activoService.obtenerTodosProveedores());
            model.addAttribute("usuarios", usuarioRepository.findAll());

            return "activos/nuevo";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos";
        }
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id, Activo activo, RedirectAttributes flash) {
        try {
            Activo activoActualizado = activoService.actualizar(id, activo);
            flash.addFlashAttribute("success",
                "Activo '" + activoActualizado.getActivoNom() + "' actualizado exitosamente");
            return "redirect:/activos/" + id;
        } catch (ActivoService.ActivoNoEncontradoException | ActivoService.ActivoInvalidoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/editar/" + id;
        }
    }

    @GetMapping("/{id}/cambiar-estado")
    public String cambiarEstadoForm(@PathVariable Long id, Model model, Principal principal) {
        try {
            validarPermisos(principal);
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("estadosPosibles", estadoTransicionService.obtenerEstadosPosibles(id));

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
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            activoService.cambiarEstado(id, nuevoEstadoId, motivo, observaciones, usuario);
            flash.addFlashAttribute("success", "Estado cambiado exitosamente");
            return "redirect:/activos/" + id;
        } catch (EstadoTransicionService.TransicionInvalidaException e) {
            flash.addFlashAttribute("error", "Transicion invalida: " + e.getMessage());
            return "redirect:/activos/" + id + "/cambiar-estado";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
        }
    }

    @PostMapping("/{id}/dar-baja")
    public String darBaja(
            @PathVariable Long id,
            @RequestParam String motivo,
            @RequestParam(required = false) String observaciones,
            Principal principal,
            RedirectAttributes flash) {
        try {
            Usuario usuario = validarEsAdministrador(principal);
            EstadoActivo estadoBaja = activoService.obtenerTodosEstados().stream()
                .filter(e -> "Dado de baja".equals(e.getEstadoNom()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Estado 'Dado de baja' no encontrado"));

            activoService.cambiarEstado(id, estadoBaja.getEstadoId(), motivo, observaciones, usuario);
            flash.addFlashAttribute("success", "Activo dado de baja exitosamente");
            return "redirect:/activos/" + id;
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
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

    @GetMapping("/{id}/historial")
    public String verHistorial(@PathVariable Long id, Model model) {
        try {
            Activo activo = activoService.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("historial", estadoTransicionService.obtenerHistorialEstados(id));

            return "activos/historial";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos/" + id;
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

    private EstadoActivo buscarEstadoPorNombre(String estadoNom) {
        return estadoActivoRepository.findByEstadoNom(estadoNom)
            .orElseThrow(() -> new RuntimeException("Estado '" + estadoNom + "' no encontrado"));
    }

    private void validarEstadoInicial(EstadoActivo estado) {
        String nombre = estado.getEstadoNom();
        if (!"Disponible".equalsIgnoreCase(nombre) && !"Asignado".equalsIgnoreCase(nombre)) {
            throw new RuntimeException("Estado inicial no permitido: " + nombre);
        }
    }

}
