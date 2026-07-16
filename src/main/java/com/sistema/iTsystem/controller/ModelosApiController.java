package com.sistema.iTsystem.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sistema.iTsystem.model.Modelo;
import com.sistema.iTsystem.service.ActivoService;

@RestController
public class ModelosApiController {

    @Autowired
    private ActivoService activoService;

    @GetMapping("/api/modelos")
    public ResponseEntity<List<Modelo>> getModelosPorMarca(@RequestParam(required = false) Long marcaId) {
        if (marcaId != null) {
            return ResponseEntity.ok(activoService.obtenerModelosPorMarca(marcaId));
        }
        return ResponseEntity.ok(activoService.obtenerTodosModelos());
    }
}
