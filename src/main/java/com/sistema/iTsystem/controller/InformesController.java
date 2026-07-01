package com.sistema.iTsystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.SolicitudesService;

@Controller
@RequestMapping("/informes")
public class InformesController {

    @Autowired
    private ActivoService activoService;

    @Autowired
    private SolicitudesService solicitudesService;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    @GetMapping
    public String listar(Model model) {
        Map<String, Long> estados = new HashMap<>();
        for (Object[] fila : activoService.contarPorEstado()) {
            estados.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }

        model.addAttribute("activosPorCategoria", activoService.contarPorCategoria());
        model.addAttribute("activosPorEstado", estados);
        model.addAttribute("totalActivos", activoService.contarTodos());
        model.addAttribute("asignacionesActivas", usuarioAsignacionRepository.findByAsignacionActivaTrueOrderByAsignacionFechaDesc().size());
        model.addAttribute("incidenciasAbiertas", solicitudesService.contarPendientes());
        model.addAttribute("ultimasSolicitudes", solicitudesService.obtenerUltimas(5));
        return "informes";
    }
}
