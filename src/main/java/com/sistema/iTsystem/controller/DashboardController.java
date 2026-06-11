package com.sistema.iTsystem.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioAsignacionRepository;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.HardwareInfoService;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private ActivoService activoService;

    @Autowired
    private HardwareInfoService hardwareService;

    @Autowired
    private UsuarioAsignacionRepository usuarioAsignacionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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

        model.addAttribute("totalActivos", activoService.obtenerTodos().size());
        model.addAttribute("totalHardware", hardwareService.obtenerTodos().size());
        model.addAttribute("hardwareDisponible", hardwareService.buscarDisponibles().size());
        model.addAttribute("hardwareEnUso", hardwareService.buscarEnUso().size());
        model.addAttribute("hardwareEnMantenimiento", hardwareService.buscarEnMantenimiento().size());
        model.addAttribute("hardwareFueraServicio", hardwareService.buscarFueraDeServicio().size());
        model.addAttribute("activosPorCategoria", activoService.contarPorCategoria());
        model.addAttribute("activosPorEstado", activoService.contarPorEstado());
        model.addAttribute("activosPorDepartamento", activoService.contarPorDepartamento());
        model.addAttribute("hardwarePorMarca", hardwareService.contarPorTipo());
        model.addAttribute("hardwarePorModelo", hardwareService.contarPorModelo());
        model.addAttribute("hardwarePorEstado", hardwareService.contarPorEstado());
        model.addAttribute("hardwarePorProveedor", hardwareService.contarPorProveedor());
        model.addAttribute("asignacionesActivas", usuarioAsignacionRepository.findAll().stream()
            .filter(asignacion -> Boolean.TRUE.equals(asignacion.getAsignacionActiva()))
            .count());

        model.addAttribute("totalSoftware", 0);
        model.addAttribute("totalLicencias", 0);
        model.addAttribute("totalMantenimientos", 0);
        model.addAttribute("totalSolicitudes", 0);
        model.addAttribute("totalEventos", 0);
        model.addAttribute("costoTotalGeneral", java.math.BigDecimal.ZERO);
    }
}
