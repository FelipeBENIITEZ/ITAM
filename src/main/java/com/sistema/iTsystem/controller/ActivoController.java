package com.sistema.iTsystem.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.CategoriasActivo;
import com.sistema.iTsystem.model.ContratoInfo;
import com.sistema.iTsystem.model.Departamentos;
import com.sistema.iTsystem.model.Garantia;
import com.sistema.iTsystem.model.HardwareCostos;
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.PresupuestoAreas;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.ContratoInfoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
import com.sistema.iTsystem.repository.GarantiaRepository;
import com.sistema.iTsystem.repository.HardwareCostosRepository;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.MarcaRepository;
import com.sistema.iTsystem.repository.ModeloRepository;
import com.sistema.iTsystem.repository.PresupuestoAreasRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;


@Controller
@RequestMapping("/activos")
public class ActivoController {

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private CategoriasActivoRepository categoriasRepository;

    @Autowired
    private EstadoActivoRepository estadoRepository;

    @Autowired
    private HardwareInfoRepository hardwareInfoRepository;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;
    @Autowired
    private PresupuestoAreasRepository presupuestoAreasRepository;

    @Autowired
    private HardwareCostosRepository hardwareCostosRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ModeloRepository modeloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ContratoInfoRepository contratoInfoRepository;

    @Autowired
    private GarantiaRepository garantiaRepository;

    @GetMapping
    public String listarActivos(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "categoria", required = false) Long categoria,
            @RequestParam(value = "estado", required = false) Long estado,
            Model model) {

        try {
            // Paginacion para ver 10 activos por pagina
            Pageable pageable = PageRequest.of(pagina, 10, Sort.by("activoFechaIngreso").descending());
            
            // Obtener activos con filtros
            Page<Activo> paginaActivos;
            
            if (buscar != null || categoria != null || estado != null) {
                // Aplicar filtros
                paginaActivos = activoRepository.findByFiltros(buscar, categoria, estado, pageable);
            } else {
                // Sin filtros - mostrar todos
                paginaActivos = activoRepository.findAll(pageable);
            }

            // Agregar datos al modelo
            model.addAttribute("activos", paginaActivos.getContent());
            model.addAttribute("paginaActual", pagina);
            model.addAttribute("totalPaginas", paginaActivos.getTotalPages());
            model.addAttribute("totalElementos", paginaActivos.getTotalElements());
            
            // Datos para filtros
            model.addAttribute("categorias", categoriasRepository.findAllByOrderByCatNomAsc());
            model.addAttribute("estados", estadoRepository.findAllByOrderByEstadoNomAsc());
            
            // Mantener valores de filtros en el formulario
            model.addAttribute("buscar", buscar);
            model.addAttribute("categoria", categoria);
            model.addAttribute("estado", estado);

            return "activos";
            
        } catch (Exception e) {
            // Log del error para debugging
            System.err.println("Error al listar activos: " + e.getMessage());
            e.printStackTrace();
            
            // Enviar listas vacías en caso de error
            model.addAttribute("activos", java.util.Collections.emptyList());
            model.addAttribute("categorias", java.util.Collections.emptyList());
            model.addAttribute("estados", java.util.Collections.emptyList());
            model.addAttribute("error", "Error al cargar los activos");
            
            return "activos";
        }
    }

    @GetMapping("/{id}")
    public String verDetalleActivo(@PathVariable Long id, Model model) {
        try {
            // Buscar el activo por ID
            Optional<Activo> activoOpt = activoRepository.findById(id);
            
            if (activoOpt.isEmpty()) {
                model.addAttribute("error", "Activo no encontrado");
                return "redirect:/activos";
            }

            Activo activo = activoOpt.get();
            
            // Obtener información de hardware con todas las relaciones
            Optional<HardwareInfo> hardwareInfoOpt = hardwareInfoRepository.findByActivoIdWithDetails(id);
            
            // Obtener asignaciones de usuarios
            List<UsuarioAsignacion> asignaciones = usuarioAsignacionRepository.findByActivoIdWithUserDetails(id);

            if (hardwareInfoOpt.isPresent()) {
                Optional<Garantia> garantiaOpt = garantiaRepository.findByHardwareInfoId(hardwareInfoOpt.get().getHwId());
                model.addAttribute("garantia", garantiaOpt.orElse(null));
            }
            // Agregar datos al modelo
            model.addAttribute("activo", activo);
            model.addAttribute("hardwareInfo", hardwareInfoOpt.orElse(null));
            model.addAttribute("asignaciones", asignaciones);
            
            return "activo-detalle";
            
        } catch (Exception e) {
            System.err.println("Error al cargar detalles del activo: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar los detalles del activo");
            return "redirect:/activos";
        }
    }

    @GetMapping("/nuevo")
    public String nuevoActivoForm(Model model, Principal principal) {
    try {
        // Obtener usuario actual
        Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        // Verificar si es administrador (rol_id = 1)
        boolean esAdministrador = usuario.getRol().getRolId().equals(1L);
        
        if (esAdministrador) {
            // ADMINISTRADOR: Obtener departamentos que tienen presupuesto vigente
            List<PresupuestoAreas> presupuestosVigentes = 
                presupuestoAreasRepository.findPresupuestosVigentesConDepartamento(LocalDate.now());
            
            if (presupuestosVigentes.isEmpty()) {
                model.addAttribute("error", "No hay departamentos con presupuesto vigente");
                return "activos/sin-presupuesto";
            }
            
            // Extraer solo los departamentos con presupuesto (para mostrar en dropdown)
            List<Departamentos> departamentosConPresupuesto = presupuestosVigentes.stream()
                .map(p -> p.getDepartamento())
                .collect(Collectors.toList());
            
            // Mantener también la lista de presupuestos para mostrar información
            model.addAttribute("departamentosDisponibles", departamentosConPresupuesto);
            model.addAttribute("presupuestosInfo", presupuestosVigentes); // Para mostrar info de presupuesto
            model.addAttribute("esAdministrador", true);
            
        } else {
            // USUARIO NORMAL: Solo su departamento
            Optional<PresupuestoAreas> presupuestoVigente = 
                presupuestoAreasRepository.findPresupuestoVigentePorDepartamento(
                    usuario.getDepartamento().getDeptId(), LocalDate.now());
            
            if (presupuestoVigente.isEmpty()) {
                model.addAttribute("error", "No hay presupuesto vigente para el departamento: " 
                    + usuario.getDepartamento().getDeptNom());
                return "activos/sin-presupuesto";
            }
            
            model.addAttribute("presupuestoActual", presupuestoVigente.get());
            model.addAttribute("esAdministrador", false);
        }
        
        // Datos comunes para todos los usuarios
        model.addAttribute("activo", new Activo());
        model.addAttribute("categorias", categoriasRepository.findAllByOrderByCatNomAsc());
        model.addAttribute("estados", estadoRepository.findAllByOrderByEstadoNomAsc());
        model.addAttribute("marcas", marcaRepository.findAllByOrderByMarcaNomAsc());
        model.addAttribute("modelos", modeloRepository.findAllByOrderByModelNomAsc());
        
        // Agregar contratos vigentes
        model.addAttribute("contratos", contratoInfoRepository.findContratosVigentesConProveedor(LocalDate.now()));
        
        return "activos/nuevo";
        
    } catch (Exception e) {
        model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
        return "error";
    }
    }   

    @PostMapping("/guardar")
    @Transactional
    public String guardarActivo(
    @RequestParam String activoNom,
    @RequestParam Long categoriaId,
    @RequestParam Long estadoId,
    @RequestParam Long departamentoId, // CAMBIO: Recibir departamentoId en lugar de presupuestoId
    @RequestParam(required = false) Long contratoId,
    @RequestParam(required = false) Long modeloId,
    @RequestParam(required = false) String numeroSerie,
    @RequestParam(required = false) BigDecimal valorCompra,
    @RequestParam(required = false) LocalDate garantFechaInicio,
    @RequestParam(required = false) Integer garantDuracionMeses,
    Principal principal,
    RedirectAttributes redirectAttributes) {

    try {
        // PASO 1: Validaciones básicas
        if (activoNom == null || activoNom.trim().isEmpty()) {
            throw new RuntimeException("El nombre del activo es obligatorio");
        }
    
        // PASO 2: Obtener usuario actual
        Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
        .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    
        boolean esAdministrador = usuario.getRol().getRolId().equals(1L);
        
        // PASO 3: Obtener presupuesto basado en el departamento
        PresupuestoAreas presupuesto;
        
        if (esAdministrador) {
            // ADMINISTRADOR: Buscar presupuesto vigente del departamento seleccionado
            presupuesto = presupuestoAreasRepository
                .findPresupuestoVigentePorDepartamento(departamentoId, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No hay presupuesto vigente para el departamento seleccionado"));
        } else {
            // USUARIO NORMAL: Verificar que el departamento corresponda al suyo
            if (!departamentoId.equals(usuario.getDepartamento().getDeptId())) {
                throw new RuntimeException("No tiene permisos para asignar activos a este departamento");
            }
            
            presupuesto = presupuestoAreasRepository
                .findPresupuestoVigentePorDepartamento(departamentoId, LocalDate.now())
                .orElseThrow(() -> new RuntimeException("No hay presupuesto vigente para su departamento"));
        }
        
        // PASO 4: Obtener contrato si se especificó
        ContratoInfo contrato = null;
        if (contratoId != null) {
            contrato = contratoInfoRepository.findById(contratoId)
                .orElseThrow(() -> new RuntimeException("Contrato no encontrado"));
            
            if (contrato.getContratFechaFin() != null && 
                contrato.getContratFechaFin().isBefore(LocalDate.now())) {
                throw new RuntimeException("No se puede asignar un contrato vencido");
            }
        }
        
        // PASO 5: Verificar presupuesto disponible
        if (valorCompra != null && valorCompra.compareTo(presupuesto.getPresupuestoDisponible()) > 0) {
            throw new RuntimeException("El valor ($" + valorCompra + ") excede el presupuesto disponible ($" 
                + presupuesto.getPresupuestoDisponible() + ") del departamento: " 
                + presupuesto.getDepartamento().getDeptNom());
        }
    
        // PASO 6: Crear y guardar el ACTIVO básico
        Activo activo = new Activo();
        activo.setActivoNom(activoNom);
        activo.setCategoria(categoriasRepository.findById(categoriaId).orElseThrow());
        activo.setEstado(estadoRepository.findById(estadoId).orElseThrow());
        activo.setDepartamento(presupuesto.getDepartamento()); // El departamento del presupuesto
        activo.setContrato(contrato);
        activo.setActivoFechaIngreso(LocalDateTime.now());
    
        Activo activoGuardado = activoRepository.save(activo);
        
        // PASO 7: Si tiene valor de compra, crear registro de costos
        HardwareInfo hardwareGuardado = null;
        if (valorCompra != null && valorCompra.compareTo(BigDecimal.ZERO) > 0) {
            HardwareInfo hardwareInfo = new HardwareInfo();
            hardwareInfo.setActivo(activoGuardado);
            if (modeloId != null) {
                hardwareInfo.setModelo(modeloRepository.findById(modeloId).orElse(null));
            }
            if (numeroSerie != null && !numeroSerie.trim().isEmpty()) {
                hardwareInfo.setHwSerialNum(numeroSerie.trim());
            }
            
            hardwareGuardado = hardwareInfoRepository.save(hardwareInfo);
            
            HardwareCostos costos = new HardwareCostos();
            costos.setHardwareInfo(hardwareGuardado);
            costos.setPresupuesto(presupuesto);
            costos.setHwValorCompra(valorCompra);
            hardwareCostosRepository.save(costos);
            
            presupuesto.setPresUsado(presupuesto.getPresUsado().add(valorCompra));
            presupuestoAreasRepository.save(presupuesto);
        }
        // PASO 8: NUEVO - Guardar Garantía si se proporcionaron datos
        if (hardwareGuardado != null && garantFechaInicio != null && 
            garantDuracionMeses != null && garantDuracionMeses > 0) {
            
            Garantia garantia = new Garantia();
            garantia.setHardwareInfo(hardwareGuardado);
            garantia.setGarantFechaInicio(garantFechaInicio);
            garantia.setGarantFechaFin(garantFechaInicio.plusMonths(garantDuracionMeses));
            
            garantiaRepository.save(garantia);
        }
        String mensaje = "Activo registrado exitosamente";
        if (contrato != null) {
            mensaje += " con contrato: " + contrato.getContratNom();
        }
        redirectAttributes.addFlashAttribute("mensaje", mensaje);
        
        if (esAdministrador) {
            redirectAttributes.addFlashAttribute("detalleAsignacion", 
                "Asignado al departamento: " + presupuesto.getDepartamento().getDeptNom());
        }
        
        return "redirect:/activos";
        
    } catch (Exception e) {
        redirectAttributes.addFlashAttribute("error", e.getMessage());
        return "redirect:/activos/nuevo";
    }
}
    private boolean esHardware(Long categoriaId) {
    CategoriasActivo categoria = categoriasRepository.findById(categoriaId).orElse(null);
    return categoria != null && categoria.getCatNom().toLowerCase().contains("hardware");
    }
}