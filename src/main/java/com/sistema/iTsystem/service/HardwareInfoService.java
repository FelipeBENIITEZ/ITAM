package com.sistema.iTsystem.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Marca;
import com.sistema.iTsystem.model.Modelo;
import com.sistema.iTsystem.model.Proveedores;
import com.sistema.iTsystem.repository.HardwareInfoRepository;
import com.sistema.iTsystem.repository.MarcaRepository;
import com.sistema.iTsystem.repository.ModeloRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;

@Service
public class HardwareInfoService {

    @Autowired
    private HardwareInfoRepository hardwareRepository;

    @Autowired
    private ModeloRepository modeloRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    public List<HardwareInfo> obtenerTodos() {
        return hardwareRepository.findAll();
    }

    public Optional<HardwareInfo> buscarPorId(Long id) {
        return hardwareRepository.findById(id);
    }

    public Optional<HardwareInfo> buscarPorIdConDetalles(Long id) {
        return hardwareRepository.findByIdWithDetails(id);
    }

    public Optional<HardwareInfo> buscarPorActivoId(Long activoId) {
        return hardwareRepository.findByActivo_ActivoId(activoId);
    }

    public Optional<HardwareInfo> buscarPorActivoIdConDetalles(Long activoId) {
        return hardwareRepository.findByActivoIdWithDetails(activoId);
    }

    public Optional<HardwareInfo> buscarPorSerial(String serial) {
        return hardwareRepository.findByHwSerialNum(serial);
    }

    @Transactional
    public HardwareInfo crear(HardwareInfo hardware) {
        normalizarHardware(hardware);
        validarHardware(hardware);

        if (hardwareRepository.existsByHwSerialNum(hardware.getHwSerialNum())) {
            throw new SerialDuplicadoException("Ya existe un hardware con el serial: " + hardware.getHwSerialNum());
        }

        return hardwareRepository.save(hardware);
    }

    @Transactional
    public HardwareInfo actualizar(Long id, HardwareInfo hardwareActualizado) {
        HardwareInfo hardwareExistente = hardwareRepository.findById(id)
            .orElseThrow(() -> new HardwareNoEncontradoException("Hardware con ID " + id + " no encontrado"));

        normalizarHardware(hardwareActualizado);
        validarHardware(hardwareActualizado);

        if (!hardwareExistente.getHwSerialNum().equals(hardwareActualizado.getHwSerialNum())
                && hardwareRepository.existsByHwSerialNum(hardwareActualizado.getHwSerialNum())) {
            throw new SerialDuplicadoException(
                "Ya existe un hardware con el serial: " + hardwareActualizado.getHwSerialNum()
            );
        }

        hardwareExistente.setHwSerialNum(hardwareActualizado.getHwSerialNum());
        hardwareExistente.setHwDescri(hardwareActualizado.getHwDescri());
        hardwareExistente.setModelo(hardwareActualizado.getModelo());

        return hardwareRepository.save(hardwareExistente);
    }

    @Transactional
    public void eliminar(Long id) {
        HardwareInfo hardware = hardwareRepository.findById(id)
            .orElseThrow(() -> new HardwareNoEncontradoException("Hardware con ID " + id + " no encontrado"));
        hardwareRepository.delete(hardware);
    }

    public List<HardwareInfo> buscarPorModelo(Long modeloId) {
        return hardwareRepository.findByModelo_ModelId(modeloId);
    }

    public List<HardwareInfo> buscarPorProveedor(Long provId) {
        return hardwareRepository.findAll().stream()
            .filter(hw -> hw.getActivo() != null)
            .filter(hw -> hw.getActivo().getProveedor() != null)
            .filter(hw -> hw.getActivo().getProveedor().getProvId().equals(provId))
            .toList();
    }

    public List<HardwareInfo> buscarSinProveedor() {
        return hardwareRepository.findHardwareSinProveedor();
    }

    public List<HardwareInfo> buscarSinValorCompra() {
        return List.of();
    }

    public List<HardwareInfo> buscarPorValorMayorA(BigDecimal valor) {
        return List.of();
    }

    public List<HardwareInfo> buscarPorValorEntre(BigDecimal min, BigDecimal max) {
        return List.of();
    }

    public List<HardwareInfo> buscarDisponibles() {
        return buscarPorEstadoActivo("Disponible");
    }

    public List<HardwareInfo> buscarEnUso() {
        return buscarPorEstadoActivo("Asignado");
    }

    public List<HardwareInfo> buscarEnMantenimiento() {
        return buscarPorEstadoActivo("En mantenimiento");
    }

    public List<HardwareInfo> buscarProyectados() {
        return List.of();
    }

    public List<HardwareInfo> buscarDadosDeBaja() {
        return buscarPorEstadoActivo("Dado de baja");
    }

    public List<HardwareInfo> buscarFueraDeServicio() {
        return buscarPorEstadoActivo("Extraviado");
    }

    private List<HardwareInfo> buscarPorEstadoActivo(String estadoNombre) {
        return hardwareRepository.findAll().stream()
            .filter(hw -> hw.getActivo() != null)
            .filter(hw -> hw.getActivo().getEstado() != null)
            .filter(hw -> estadoNombre.equalsIgnoreCase(hw.getActivo().getEstado().getEstadoNom()))
            .toList();
    }

    public BigDecimal calcularValorTotal() {
        return BigDecimal.ZERO;
    }

    public BigDecimal calcularValorPromedio() {
        return BigDecimal.ZERO;
    }

    public List<Object[]> contarPorProveedor() {
        return hardwareRepository.countHardwarePorProveedor();
    }

    public List<Object[]> contarPorModelo() {
        return hardwareRepository.countHardwarePorModelo();
    }

    public long contarSinValorCompra() {
        return 0L;
    }

    public List<Object[]> contarPorTipo() {
        Map<String, Long> conteo = new HashMap<>();
        for (HardwareInfo hw : hardwareRepository.findAll()) {
            if (hw.getModelo() != null && hw.getModelo().getMarca() != null) {
                String marcaNombre = hw.getModelo().getMarca().getMarcaNom();
                conteo.put(marcaNombre, conteo.getOrDefault(marcaNombre, 0L) + 1);
            }
        }
        return conteo.entrySet().stream()
            .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
            .collect(Collectors.toList());
    }

    public List<Object[]> contarPorEstado() {
        return hardwareRepository.findAll().stream()
            .filter(hw -> hw.getActivo() != null && hw.getActivo().getEstado() != null)
            .collect(Collectors.groupingBy(
                hw -> hw.getActivo().getEstado().getEstadoNom(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
            .toList();
    }

    public boolean existeSerial(String serial) {
        return hardwareRepository.existsByHwSerialNum(serial);
    }

    public boolean tieneGarantiaVigente(HardwareInfo hardware) {
        return hardware.getGarantia() != null && hardware.getGarantia().isVigente();
    }

    public List<Modelo> obtenerTodosModelos() {
        return modeloRepository.findAll();
    }

    public List<Marca> obtenerTodasMarcas() {
        return marcaRepository.findAllByOrderByMarcaNomAsc();
    }

    public List<Proveedores> obtenerTodosProveedores() {
        return proveedoresRepository.findAll();
    }

    private void validarHardware(HardwareInfo hardware) {
        if (hardware.getHwSerialNum() == null || hardware.getHwSerialNum().trim().isEmpty()) {
            throw new HardwareInvalidoException("El numero de serie es obligatorio");
        }
        if (hardware.getModelo() == null) {
            throw new HardwareInvalidoException("El modelo es obligatorio");
        }
        if (hardware.getActivo() == null) {
            throw new HardwareInvalidoException("El activo asociado es obligatorio");
        }
    }

    private void normalizarHardware(HardwareInfo hardware) {
        if (hardware == null) {
            return;
        }

        if (hardware.getHwSerialNum() != null) {
            hardware.setHwSerialNum(hardware.getHwSerialNum().trim());
        }
    }

    public static class HardwareNoEncontradoException extends RuntimeException {
        public HardwareNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class HardwareInvalidoException extends RuntimeException {
        public HardwareInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class SerialDuplicadoException extends RuntimeException {
        public SerialDuplicadoException(String mensaje) {
            super(mensaje);
        }
    }
}
