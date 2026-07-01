package com.sistema.iTsystem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping
public class ModuloSimpleController {

    @GetMapping("/gestion")
    public String gestion(Model model) {
        prepararModulo(model, "Gestión", "Espacio reservado para procesos internos, aprobaciones y seguimiento operativo.");
        return "modulo-simple";
    }

    @GetMapping("/administracion")
    public String administracion(Model model) {
        prepararModulo(model, "Administración", "Aquí se concentrarán las funciones administrativas y de control de acceso.");
        return "modulo-simple";
    }

    @GetMapping("/configuracion")
    public String configuracion(Model model) {
        prepararModulo(model, "Configuración", "Pantalla simple para parámetros generales y catálogos complementarios.");
        return "modulo-simple";
    }

    private void prepararModulo(Model model, String titulo, String descripcion) {
        model.addAttribute("tituloModulo", titulo);
        model.addAttribute("descripcionModulo", descripcion);
    }
}
