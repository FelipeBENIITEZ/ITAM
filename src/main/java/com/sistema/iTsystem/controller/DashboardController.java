package com.sistema.iTsystem.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.PersonaRepository;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.SolicitudesService;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private ActivoService activoService;

    @Autowired
    private SolicitudesService solicitudesService;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @GetMapping
    public String dashboard(Principal principal, Model model) {
        cargarMetricasDemo(principal, model);
        return "dashboard";
    }

    @GetMapping("/activos")
    public String dashboardActivos(Principal principal, Model model) {
        cargarMetricasDemo(principal, model);
        return "dashboard";
    }

    @GetMapping("/hardware")
    public String dashboardHardware(Principal principal, Model model) {
        cargarMetricasDemo(principal, model);
        return "dashboard";
    }

    @GetMapping("/software-licencias")
    public String dashboardSoftwareLicencias() {
        return "redirect:/dashboard";
    }

    @GetMapping("/mantenimientos")
    public String dashboardMantenimientos() {
        return "redirect:/dashboard";
    }

    @GetMapping("/solicitudes")
    public String dashboardSolicitudes() {
        return "redirect:/dashboard";
    }

    @GetMapping("/eventos")
    public String dashboardEventos() {
        return "redirect:/dashboard";
    }

    @GetMapping("/financiero")
    public String dashboardFinanciero() {
        return "redirect:/dashboard";
    }

    @GetMapping("/alertas")
    public String dashboardAlertas() {
        return "redirect:/dashboard";
    }

    private void cargarMetricasDemo(Principal principal, Model model) {
        if (principal != null) {
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName()).orElse(null);
            model.addAttribute("usuario", usuario);
            model.addAttribute("esAdmin", usuario != null && usuario.esAdministrador());
        }

        Map<String, Long> estados = new HashMap<>();
        for (Object[] fila : activoService.contarPorEstado()) {
            estados.put(String.valueOf(fila[0]), ((Number) fila[1]).longValue());
        }

        model.addAttribute("totalActivos", activoService.contarTodos());
        model.addAttribute("activosDisponibles", estados.getOrDefault("Disponible", 0L));
        model.addAttribute("activosAsignados", estados.getOrDefault("Asignado", 0L));
        model.addAttribute("activosEnMantenimiento", estados.getOrDefault("En mantenimiento", 0L));
        model.addAttribute("activosDadosDeBaja", estados.getOrDefault("Dado de baja", 0L));
        model.addAttribute("totalUsuarios", usuarioRepository.count());
        model.addAttribute("totalPersonas", personaRepository.count());
        model.addAttribute("incidenciasAbiertas", solicitudesService.contarPendientes());
        model.addAttribute("asignacionesActivas", usuarioAsignacionRepository.findByAsignacionActivaTrueOrderByAsignacionFechaDesc().size());
        model.addAttribute("activosPorCategoria", activoService.contarPorCategoria());
        model.addAttribute("activosPorEstado", estados);
        model.addAttribute("totalSolicitudes", solicitudesService.obtenerTodas().size());
    }
}
