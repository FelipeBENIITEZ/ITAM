package com.sistema.iTsystem.controller;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.CategoriasActivo;
import com.sistema.iTsystem.model.HardwareCostos;
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.PresupuestoAreas;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.sistema.iTsystem.repository.PresupuestoAreasRepository;
import com.sistema.iTsystem.repository.HardwareCostosRepository;
import com.sistema.iTsystem.repository.MarcaRepository;
import com.sistema.iTsystem.repository.ModeloRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @GetMapping
    public String listarActivos(
            @RequestParam(value = "pagina", defaultValue = "0") int pagina,
            @RequestParam(value = "buscar", required = false) String buscar,
            @RequestParam(value = "categoria", required = false) Long categoria,
            @RequestParam(value = "estado", required = false) Long estado,
            Model model) {

        try {
            // Configurar paginación (10 activos por página)
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
        
        // Buscar presupuesto vigente del departamento del usuario
        Optional<PresupuestoAreas> presupuestoVigente = 
            presupuestoAreasRepository.findPresupuestoVigentePorDepartamento(
                usuario.getDepartamento().getDeptId(), LocalDate.now());
        
        if (presupuestoVigente.isEmpty()) {
            model.addAttribute("error", "No hay presupuesto vigente para el departamento: " 
                + usuario.getDepartamento().getDeptNom());
            return "activos/sin-presupuesto";
        }
        
        // Agregar datos al modelo
        model.addAttribute("activo", new Activo());
        model.addAttribute("presupuestoActual", presupuestoVigente.get());
        model.addAttribute("categorias", categoriasRepository.findAllByOrderByCatNomAsc());
        model.addAttribute("estados", estadoRepository.findAllByOrderByEstadoNomAsc());
        model.addAttribute("marcas", marcaRepository.findAllByOrderByMarcaNomAsc());
        model.addAttribute("modelos", modeloRepository.findAllByOrderByModelNomAsc());
        
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
        @RequestParam Long presupuestoId,
        @RequestParam(required = false) Long modeloId,
        @RequestParam(required = false) String numeroSerie,
        @RequestParam(required = false) BigDecimal valorCompra,
        Principal principal,
        RedirectAttributes redirectAttributes) {
    
        try {
            // PASO 1: Validaciones básicas
            if (activoNom == null || activoNom.trim().isEmpty()) {
                throw new RuntimeException("El nombre del activo es obligatorio");
            }
        
            // PASO 2: Obtener usuario actual y su departamento
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
            // PASO 3: Verificar presupuesto disponible (solo para hardware)
            PresupuestoAreas presupuesto = presupuestoAreasRepository.findById(presupuestoId)
            .orElseThrow(() -> new RuntimeException("Presupuesto no encontrado"));
        
            if (valorCompra != null && valorCompra.compareTo(presupuesto.getPresupuestoDisponible()) > 0) {
                throw new RuntimeException("El valor excede el presupuesto disponible");
            }
        
            // PASO 4: Crear y guardar el ACTIVO básico
            Activo activo = new Activo();
            activo.setActivoNom(activoNom);
            activo.setCategoria(categoriasRepository.findById(categoriaId).orElseThrow());
            activo.setEstado(estadoRepository.findById(estadoId).orElseThrow());
            activo.setDepartamento(usuario.getDepartamento());
            activo.setActivoFechaIngreso(LocalDateTime.now());
        
            Activo activoGuardado = activoRepository.save(activo);
        
            // PASO 5: Si es categoría hardware, crear HARDWARE_INFO
            if (esHardware(categoriaId)) {
                if (modeloId == null) {
                throw new RuntimeException("El modelo es obligatorio para hardware");
            }
            
                HardwareInfo hardwareInfo = new HardwareInfo();
                hardwareInfo.setActivo(activoGuardado);
                hardwareInfo.setModelo(modeloRepository.findById(modeloId).orElseThrow());
                hardwareInfo.setHwSerialNum(numeroSerie);
            
                HardwareInfo hardwareGuardado = hardwareInfoRepository.save(hardwareInfo);
            
                // PASO 6: Crear HARDWARE_COSTOS
                if (valorCompra != null && valorCompra.compareTo(BigDecimal.ZERO) > 0) {
                    HardwareCostos costos = new HardwareCostos();
                    costos.setHardwareInfo(hardwareGuardado);
                    costos.setPresupuesto(presupuesto);
                    costos.setHwValorCompra(valorCompra);
                    hardwareCostosRepository.save(costos);
                
                    // PASO 7: Actualizar presupuesto usado
                    presupuesto.setPresUsado(presupuesto.getPresUsado().add(valorCompra));
                    presupuestoAreasRepository.save(presupuesto);
                }
            }
        
            redirectAttributes.addFlashAttribute("success", "Activo creado correctamente");
            return "redirect:/activos";
        
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar: " + e.getMessage());
            return "redirect:/activos/nuevo";
        }
    }

        // Método auxiliar para verificar si es categoría hardware
        private boolean esHardware(Long categoriaId) {
        CategoriasActivo categoria = categoriasRepository.findById(categoriaId).orElse(null);
        return categoria != null && categoria.getCatNom().toLowerCase().contains("hardware");
}
}