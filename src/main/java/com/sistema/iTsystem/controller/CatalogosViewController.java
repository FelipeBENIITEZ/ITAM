package com.sistema.iTsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.HardwareInfoService;

@Controller
@RequestMapping("/catalogos")
public class CatalogosViewController {

    @Autowired
    private ActivoService activoService;

    @Autowired
    private HardwareInfoService hardwareService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("categorias", activoService.obtenerTodasCategorias());
        model.addAttribute("estados", activoService.obtenerTodosEstados());
        model.addAttribute("marcas", hardwareService.obtenerTodasMarcas());
        model.addAttribute("modelos", hardwareService.obtenerTodosModelos());
        model.addAttribute("proveedores", hardwareService.obtenerTodosProveedores());
        return "catalogos";
    }
}
