package com.sistema.iTsystem.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.text.Normalizer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
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
import com.sistema.iTsystem.model.Garantia;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.EstadoTransicionService;
import com.sistema.iTsystem.service.HistorialActivoService;
import com.sistema.iTsystem.service.HardwareInfoService;
import com.sistema.iTsystem.service.MovimientosService;

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
    private MovimientosService movimientosService;

    @Autowired
    private HistorialActivoService historialActivoService;

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
            Model model) {

        try {
            Pageable pageable = PageRequest.of(pagina, 10, Sort.by("activoFechaIngreso").descending());
            Page<Activo> paginaActivos = activoService.buscarConFiltros(
                buscar, categoria, estado, null, pageable
            );

            model.addAttribute("activos", paginaActivos.getContent());
            model.addAttribute("paginaActual", pagina);
            model.addAttribute("totalPaginas", paginaActivos.getTotalPages());
            model.addAttribute("totalElementos", paginaActivos.getTotalElements());
            model.addAttribute("categorias", activoService.obtenerTodasCategorias());
            model.addAttribute("estados", activoService.obtenerTodosEstados());
            model.addAttribute("buscar", buscar);
            model.addAttribute("categoria", categoria);
            model.addAttribute("estado", estado);

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

            cargarDetalleActivo(model, activo, hardwareService.buscarPorActivoIdConDetalles(activo.getActivoId()).orElse(null),
                movimientosService.obtenerAsignacionActiva(activo.getActivoId()).orElse(null),
                false, null, null, null, null);
            return "activo-detalle";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/activos";
        }
    }

    @GetMapping("/{activoCodigo}/asignar")
    public String formularioAsignacion(@PathVariable String activoCodigo, Model model, RedirectAttributes flash) {
        try {
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            UsuarioAsignacion asignacionActiva = movimientosService.obtenerAsignacionActiva(activo.getActivoId()).orElse(null);
            if (asignacionActiva != null || activo.getEstado() == null
                    || !"Disponible".equalsIgnoreCase(activo.getEstado().getEstadoNom())) {
                flash.addFlashAttribute("error", "El activo no esta disponible para asignacion.");
                return "redirect:/activos/" + activo.getActivoCodigo();
            }

            cargarFormularioAsignacion(model, activo, asignacionActiva, null, null, null, null, null);
            return "activos/asignar";
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos";
        }
    }

    @PostMapping("/{activoCodigo}/asignar")
    public String asignarActivo(
            @PathVariable String activoCodigo,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaAsignacion,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String observacion,
            Principal principal,
            Model model,
            RedirectAttributes flash) {
        try {
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            Usuario usuarioOperador = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            movimientosService.asignarActivo(
                activo.getActivoId(),
                usuarioId,
                fechaAsignacion,
                motivo,
                observacion,
                usuarioOperador
            );
            flash.addFlashAttribute("success", "Activo asignado correctamente.");
            return "redirect:/activos/" + activo.getActivoCodigo();
        } catch (Exception e) {
            try {
                Activo activo = activoService.buscarPorCodigo(activoCodigo)
                    .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

                cargarFormularioAsignacion(
                    model,
                    activo,
                    movimientosService.obtenerAsignacionActiva(activo.getActivoId()).orElse(null),
                    fechaAsignacion,
                    usuarioId,
                    motivo,
                    observacion,
                    e.getMessage()
                );
                return "activos/asignar";
            } catch (Exception detalleException) {
                flash.addFlashAttribute("error", detalleException.getMessage());
                return "redirect:/activos";
            }
        }
    }

    @PostMapping("/{activoCodigo}/garantia")
    public String guardarGarantia(
            @PathVariable String activoCodigo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String descripcion,
            Model model,
            RedirectAttributes flash) {
        try {
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new ActivoService.ActivoNoEncontradoException("Activo no encontrado"));

            HardwareInfo hardware = hardwareService.buscarPorActivoIdConDetalles(activo.getActivoId()).orElse(null);
            if (hardware == null) {
                cargarDetalleActivo(model, activo, null, null, true,
                    "No se puede cargar garantia porque el activo no tiene datos tecnicos asociados.",
                    fechaInicio, fechaFin, descripcion);
                return "activo-detalle";
            }

            hardwareService.guardarGarantia(hardware, fechaInicio, fechaFin, descripcion);
            flash.addFlashAttribute("success", "Garantia guardada exitosamente.");
            return "redirect:/activos/" + activo.getActivoCodigo();
        } catch (ActivoService.ActivoNoEncontradoException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos";
        } catch (HardwareInfoService.HardwareInvalidoException e) {
            try {
                Activo activo = activoService.buscarPorCodigo(activoCodigo)
                    .orElseThrow(() -> new ActivoService.ActivoNoEncontradoException("Activo no encontrado"));
                HardwareInfo hardware = hardwareService.buscarPorActivoIdConDetalles(activo.getActivoId()).orElse(null);
                cargarDetalleActivo(model, activo, hardware,
                    movimientosService.obtenerAsignacionActiva(activo.getActivoId()).orElse(null),
                    true, e.getMessage(), fechaInicio, fechaFin, descripcion);
                return "activo-detalle";
            } catch (Exception detalleException) {
                flash.addFlashAttribute("error", detalleException.getMessage());
                return "redirect:/activos";
            }
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/activos/" + activoCodigo;
        }
    }

    @GetMapping("/nuevo")
    public String nuevoForm(Model model) {
        prepararFormularioAlta(model, new Activo(), null, null, null, null, null);
        return "activos/nuevo";
    }

    @PostMapping("/guardar")
    public String guardar(
            Model model,
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
        Activo activo = new Activo();
        activo.setActivoCodigo(activoCodigo);
        activo.setActivoNom(activoNom);
        activo.setActivoDescri(activoDescri);

        try {
            activo.setCategoria(categoriasRepository.findById(categoriaId)
                .orElseThrow(() -> new RuntimeException("Categoria no encontrada")));

            if (proveedorId != null) {
                activo.setProveedor(proveedoresRepository.findById(proveedorId)
                    .orElseThrow(() -> new RuntimeException("Proveedor no encontrado")));
            }

            Usuario usuarioOperador = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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
            prepararFormularioAlta(model, activo, marcaId, modeloId, numeroSerie, categoriaId, proveedorId);
            asignarErroresFormulario(model, e.getMessage());
            return "activos/nuevo";
        } catch (DataIntegrityViolationException e) {
            prepararFormularioAlta(model, activo, marcaId, modeloId, numeroSerie, categoriaId, proveedorId);
            model.addAttribute("error", "Ya existe un activo con ese codigo o ese numero de serie.");
            return "activos/nuevo";
        } catch (Exception e) {
            prepararFormularioAlta(model, activo, marcaId, modeloId, numeroSerie, categoriaId, proveedorId);
            model.addAttribute("error", e.getMessage());
            return "activos/nuevo";
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

    @GetMapping("/{activoCodigo}/historial")
    public String verHistorial(@PathVariable String activoCodigo, Model model) {
        try {
            Activo activo = activoService.buscarPorCodigo(activoCodigo)
                .orElseThrow(() -> new RuntimeException("Activo no encontrado"));

            model.addAttribute("activo", activo);
            model.addAttribute("historialOperativo", historialActivoService.obtenerHistorialOperativo(activo.getActivoId()));

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
                descripcion = "El activo pasara a mantenimiento y quedara fuera de uso operativo.";
            }
            case "finalizar-mantenimiento" -> {
                titulo = "Finalizar mantenimiento";
                estadoResultante = "Disponible";
                descripcion = "El activo volvera a estar disponible para uso operativo.";
            }
            case "baja" -> {
                titulo = "Dar de baja";
                estadoResultante = "Dado de baja";
                descripcion = "La baja es logica. El activo seguira visible con su historial.";
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

    private void prepararFormularioAlta(Model model, Activo activo, Long marcaId, Long modeloId,
                                        String numeroSerie, Long categoriaId, Long proveedorId) {
        model.addAttribute("activo", activo);
        model.addAttribute("categorias", activoService.obtenerTodasCategorias());
        model.addAttribute("marcas", activoService.obtenerTodasMarcas());
        model.addAttribute("modelos", activoService.obtenerTodosModelos());
        model.addAttribute("proveedores", activoService.obtenerTodosProveedores());
        model.addAttribute("marcaId", marcaId);
        model.addAttribute("modeloId", modeloId);
        model.addAttribute("numeroSerie", numeroSerie);
        model.addAttribute("categoriaId", categoriaId);
        model.addAttribute("proveedorId", proveedorId);
        model.addAttribute("modoEdicion", false);
    }

    private void cargarDetalleActivo(Model model, Activo activo, HardwareInfo hardwareInfo,
                                     UsuarioAsignacion asignacionActiva,
                                     boolean mostrarFormularioGarantia, String errorGarantia,
                                     LocalDate garantiaFechaInicio, LocalDate garantiaFechaFin,
                                     String garantiaDescri) {
        model.addAttribute("activo", activo);
        model.addAttribute("hardwareInfo", hardwareInfo);
        model.addAttribute("asignacionActiva", asignacionActiva);
        model.addAttribute("puedeAsignar", activo.getEstado() != null
            && "Disponible".equalsIgnoreCase(activo.getEstado().getEstadoNom())
            && asignacionActiva == null);

        Garantia garantia = hardwareInfo != null ? hardwareInfo.getGarantia() : null;
        model.addAttribute("garantia", garantia);
        model.addAttribute("garantiaFechaInicio", garantiaFechaInicio != null
            ? garantiaFechaInicio
            : garantia != null ? garantia.getGaranFechaInicio() : null);
        model.addAttribute("garantiaFechaFin", garantiaFechaFin != null
            ? garantiaFechaFin
            : garantia != null ? garantia.getGaranFechaFin() : null);
        model.addAttribute("garantiaDescri", garantiaDescri != null
            ? garantiaDescri
            : garantia != null ? garantia.getGaranDescri() : null);
        model.addAttribute("mostrarFormularioGarantia", mostrarFormularioGarantia);
        if (errorGarantia != null) {
            model.addAttribute("error", errorGarantia);
        }

        model.addAttribute("historialOperativo", historialActivoService.obtenerHistorialOperativo(activo.getActivoId()));
        model.addAttribute("historialEstados", estadoTransicionService.obtenerHistorialEstados(activo.getActivoId()));
    }

    private void cargarFormularioAsignacion(Model model, Activo activo, UsuarioAsignacion asignacionActiva,
                                            LocalDate fechaAsignacion, Long usuarioId, String motivo,
                                            String observacion, String error) {
        LocalDate fechaMinimaAsignacion = activo != null && activo.getActivoFechaIngreso() != null
            ? activo.getActivoFechaIngreso().toLocalDate()
            : LocalDate.now();

        model.addAttribute("activo", activo);
        model.addAttribute("asignacionActiva", asignacionActiva);
        model.addAttribute("usuarios", movimientosService.obtenerUsuariosActivos());
        model.addAttribute("fechaMinimaAsignacion", fechaMinimaAsignacion);
        model.addAttribute("fechaAsignacion", obtenerFechaAsignacionInicial(fechaAsignacion, fechaMinimaAsignacion));
        model.addAttribute("usuarioId", usuarioId);
        model.addAttribute("motivo", motivo);
        model.addAttribute("observacion", observacion);
        model.addAttribute("error", error);
    }

    private LocalDate obtenerFechaAsignacionInicial(LocalDate fechaAsignacion, LocalDate fechaMinimaAsignacion) {
        if (fechaAsignacion != null) {
            return fechaAsignacion;
        }

        LocalDate hoy = LocalDate.now();
        if (hoy.isBefore(fechaMinimaAsignacion)) {
            return fechaMinimaAsignacion;
        }

        return hoy;
    }

    private void asignarErroresFormulario(Model model, String mensaje) {
        model.addAttribute("error", mensaje);

        String mensajeNormalizado = normalizarTexto(mensaje);
        if (mensajeNormalizado.contains("codigo")) {
            model.addAttribute("codigoError", mensaje);
        }
        if (mensajeNormalizado.contains("serial") || mensajeNormalizado.contains("numero de serie")) {
            model.addAttribute("serialError", mensaje);
        }
    }

    private String normalizarTexto(String texto) {
        if (texto == null) {
            return "";
        }

        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
        return sinAcentos.toLowerCase();
    }
}
