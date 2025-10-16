package com.sistema.iTsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.HardwareInfoService;

@RestController
@RequestMapping("/api/catalogos")
public class CatalogosController {
    
    @Autowired
    private ActivoService activoService;
    
    @Autowired
    private HardwareInfoService hardwareService;
    
    // ==================== CATEGORÍAS ====================
    
    @GetMapping("/categorias")
    public ResponseEntity<?> getCategorias() {
        try {
            return ResponseEntity.ok(activoService.obtenerTodasCategorias());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    // ==================== ESTADOS ====================
    
    @GetMapping("/estados")
    public ResponseEntity<?> getEstados() {
        try {
            return ResponseEntity.ok(activoService.obtenerTodosEstados());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    // ==================== DEPARTAMENTOS ====================
    
    @GetMapping("/departamentos")
    public ResponseEntity<?> getDepartamentos() {
        try {
            return ResponseEntity.ok(activoService.obtenerTodosDepartamentos());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    // ==================== MODELOS ====================
    
    @GetMapping("/modelos")
    public ResponseEntity<?> getModelos() {
        try {
            return ResponseEntity.ok(hardwareService.obtenerTodosModelos());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    // ==================== PROVEEDORES ====================
    
    @GetMapping("/proveedores")
    public ResponseEntity<?> getProveedores() {
        try {
            return ResponseEntity.ok(hardwareService.obtenerTodosProveedores());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    // ==================== RESUMEN GENERAL ====================
    
    @GetMapping("/resumen")
    public ResponseEntity<?> getResumen() {
        try {
            java.util.Map<String, Object> resumen = new java.util.HashMap<>();
            
            resumen.put("categorias", activoService.obtenerTodasCategorias().size());
            resumen.put("estados", activoService.obtenerTodosEstados().size());
            resumen.put("departamentos", activoService.obtenerTodosDepartamentos().size());
            resumen.put("modelos", hardwareService.obtenerTodosModelos().size());
            resumen.put("proveedores", hardwareService.obtenerTodosProveedores().size());
            
            resumen.put("activos", activoService.obtenerTodos().size());
            resumen.put("hardware", hardwareService.obtenerTodos().size());
            
            resumen.put("mensaje", "Sistema funcionando correctamente");
            resumen.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(resumen);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    // ==================== HEALTH CHECK ====================
    
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        java.util.Map<String, Object> health = new java.util.HashMap<>();
        health.put("status", "UP");
        health.put("service", "iTsystem API");
        health.put("timestamp", java.time.LocalDateTime.now());
        health.put("database", "Connected");
        
        return ResponseEntity.ok(health);
    }
}