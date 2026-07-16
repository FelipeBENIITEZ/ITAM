package com.sistema.iTsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sistema.iTsystem.service.DashboardService;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("dashboard", dashboardService.obtenerDashboard());
        return "dashboard";
    }

    @GetMapping("/activos")
    public String dashboardActivos(Model model) {
        model.addAttribute("dashboard", dashboardService.obtenerDashboard());
        return "dashboard";
    }

    @GetMapping("/hardware")
    public String dashboardHardware(Model model) {
        model.addAttribute("dashboard", dashboardService.obtenerDashboard());
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
}
