package com.sistema.iTsystem.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sistema.iTsystem.dto.activos.ActivoReferenciaDTO;
import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.UsuarioAsignacion;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.service.HardwareInfoService;
import com.sistema.iTsystem.service.MovimientosService;

@RestController
@RequestMapping("/api/activos")
public class ActivosApiController {

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private HardwareInfoService hardwareInfoService;

    @Autowired
    private MovimientosService movimientosService;

    @GetMapping("/{id}/referencia")
    public ResponseEntity<ActivoReferenciaDTO> referencia(@PathVariable Long id) {
        Optional<Activo> activoOpt = activoRepository.findById(id);
        if (activoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Activo activo = activoOpt.get();

        ActivoReferenciaDTO dto = new ActivoReferenciaDTO();
        dto.setActivoId(activo.getActivoId());
        dto.setCodigo(activo.getActivoCodigo());
        dto.setNombre(activo.getActivoNom());
        dto.setCategoria(activo.getCategoria() != null ? activo.getCategoria().getCatNom() : null);
        dto.setEstado(activo.getEstado() != null ? activo.getEstado().getEstadoNom() : null);

        Optional<HardwareInfo> hardwareOpt = hardwareInfoService.buscarPorActivoIdConDetalles(id);
        hardwareOpt.ifPresent(hardware -> {
            dto.setSerial(hardware.getHwSerialNum());
            if (hardware.getModelo() != null) {
                dto.setModeloId(hardware.getModelo().getModelId());
                dto.setModelo(hardware.getModelo().getModelNom());
                if (hardware.getModelo().getMarca() != null) {
                    dto.setMarcaId(hardware.getModelo().getMarca().getMarcaId());
                    dto.setMarca(hardware.getModelo().getMarca().getMarcaNom());
                }
            }
        });

        Optional<UsuarioAsignacion> asignacionActiva = movimientosService.obtenerAsignacionActiva(id);
        dto.setAsignado(asignacionActiva.isPresent());
        asignacionActiva.ifPresent(asignacion -> {
            if (asignacion.getUsuario() != null) {
                dto.setUsuarioActualId(asignacion.getUsuario().getUsuId());
                dto.setUsuarioActual(asignacion.getUsuario().getUsuLogin());
            }
        });

        return ResponseEntity.ok(dto);
    }
}
