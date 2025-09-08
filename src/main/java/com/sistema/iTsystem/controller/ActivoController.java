package com.sistema.iTsystem.controller;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.HardwareInfo;
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

import java.util.List;
import java.util.Optional;

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
}