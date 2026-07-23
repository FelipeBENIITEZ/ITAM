package com.sistema.iTsystem.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.MovimientosService;

@Controller
@RequestMapping("/movimientos")
public class MovimientosController {

    @Autowired
    private MovimientosService movimientosService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("asignacionesActivas", movimientosService.obtenerAsignacionesActivas());
        model.addAttribute("activosDisponibles", movimientosService.obtenerActivosDisponiblesParaAsignar());
        model.addAttribute("usuarios", movimientosService.obtenerUsuariosActivos());
        return "movimientos";
    }

    @PostMapping("/asignar")
    public String asignar(
            @RequestParam Long activoId,
            @RequestParam Long usuarioId,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String observacion,
            Principal principal,
            RedirectAttributes flash) {
        try {
            Usuario usuarioOperador = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            movimientosService.asignarActivo(activoId, usuarioId, motivo, observacion, usuarioOperador);
            flash.addFlashAttribute("success", "Activo asignado correctamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/movimientos";
    }

    @PostMapping("/devolver/{asignacionId}")
    public String devolver(
            @PathVariable Long asignacionId,
            @RequestParam(required = false) String motivo,
            @RequestParam(required = false) String observacion,
            Principal principal,
            RedirectAttributes flash) {
        try {
            Usuario usuarioOperador = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            movimientosService.devolverActivo(asignacionId, motivo, observacion, usuarioOperador);
            flash.addFlashAttribute("success", "Activo devuelto correctamente");
        } catch (Exception e) {
            flash.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/movimientos";
    }
}
