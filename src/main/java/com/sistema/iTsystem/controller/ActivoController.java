package com.sistema.iTsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;

@Controller
@RequestMapping("/activos")
public class ActivoController {

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private CategoriasActivoRepository categoriasRepository;

    @Autowired
    private EstadoActivoRepository estadoRepository;

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
}