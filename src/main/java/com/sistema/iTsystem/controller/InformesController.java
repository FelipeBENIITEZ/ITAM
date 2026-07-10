package com.sistema.iTsystem.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sistema.iTsystem.service.ActivoService;

@Controller
@RequestMapping("/informes")
public class InformesController {

    @Autowired
    private ActivoService activoService;

    @GetMapping
    public String listar(Model model) {
        Map<String, Long> estados = new LinkedHashMap<>();
        for (Object[] fila : activoService.contarPorEstado()) {
            estados.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }

        model.addAttribute("activosPorCategoria", activoService.contarPorCategoria());
        model.addAttribute("activosPorEstado", estados);
        model.addAttribute("totalActivos", activoService.contarTodos());
        model.addAttribute("activosDisponibles", estados.getOrDefault("Disponible", 0L));
        model.addAttribute("activosEnMantenimiento", estados.getOrDefault("En mantenimiento", 0L));
        model.addAttribute("activosDadosDeBaja", estados.getOrDefault("Dado de baja", 0L));
        return "informes";
    }
}
