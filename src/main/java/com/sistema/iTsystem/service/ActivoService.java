package com.sistema.iTsystem.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sistema.iTsystem.model.Activo;
import com.sistema.iTsystem.model.CategoriasActivo;
import com.sistema.iTsystem.model.Departamentos;
import com.sistema.iTsystem.model.EstadoActivo;
import com.sistema.iTsystem.model.HardwareInfo;
import com.sistema.iTsystem.model.Marca;
import com.sistema.iTsystem.model.Modelo;
import com.sistema.iTsystem.model.Proveedores;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.DepartamentosRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
import com.sistema.iTsystem.repository.MarcaRepository;
import com.sistema.iTsystem.repository.ModeloRepository;
import com.sistema.iTsystem.repository.ProveedoresRepository;

@Service
public class ActivoService {

    public static class ActivoNoEncontradoException extends RuntimeException {
        public ActivoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ActivoInvalidoException extends RuntimeException {
        public ActivoInvalidoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ActivoConDependenciasException extends RuntimeException {
        public ActivoConDependenciasException(String mensaje) {
            super(mensaje);
        }
    }

    @Autowired
    private ActivoRepository activoRepository;

    @Autowired
    private CategoriasActivoRepository categoriasRepository;

    @Autowired
    private EstadoActivoRepository estadoActivoRepository;

    @Autowired
    private DepartamentosRepository departamentosRepository;

    @Autowired
    private ProveedoresRepository proveedoresRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ModeloRepository modeloRepository;

    @Autowired
    private HardwareInfoService hardwareInfoService;

    @Autowired
    private EstadoTransicionService estadoTransicionService;

    public List<Activo> obtenerTodos() {
        return activoRepository.findAll();
    }

    public Page<Activo> obtenerTodos(Pageable pageable) {
        return activoRepository.findAll(pageable);
    }

    public Optional<Activo> buscarPorId(Long id) {
        return activoRepository.findById(id);
    }

    public Optional<Activo> buscarPorCodigo(String activoCodigo) {
        if (activoCodigo == null || activoCodigo.trim().isEmpty()) {
            return Optional.empty();
        }

        return activoRepository.findByActivoCodigoIgnoreCase(activoCodigo.trim());
    }

    @Transactional
    public Activo crear(Activo activo) {
        return crear(activo, null, null, null);
    }

    @Transactional
    public Activo crear(Activo activo, Long marcaId, Long modeloId, String numeroSerie) {
        validarActivo(activo, null);
        activo.setActivoCodigo(normalizarCodigo(activo.getActivoCodigo()));

        if (activo.getEstado() == null) {
            activo.setEstado(obtenerEstadoInicial());
        }
        if (activo.getActivoFechaIngreso() == null) {
            activo.setActivoFechaIngreso(LocalDateTime.now());
        }
        if (activo.getActivoActivo() == null) {
            activo.setActivoActivo(true);
        }

        Activo activoGuardado = activoRepository.save(activo);

        if (marcaId != null || modeloId != null || numeroSerie != null) {
            HardwareInfo hardware = crearHardwareParaActivo(activoGuardado, marcaId, modeloId, numeroSerie);
            hardwareInfoService.crear(hardware);
        }

        return activoGuardado;
    }

    @Transactional
    public Activo actualizar(Long id, Activo activoActualizado) {
        Activo activoExistente = activoRepository.findById(id)
            .orElseThrow(() -> new ActivoNoEncontradoException("Activo con ID " + id + " no encontrado"));

        validarActualizacion(activoActualizado);

        activoExistente.setActivoNom(activoActualizado.getActivoNom());
        activoExistente.setActivoDescri(activoActualizado.getActivoDescri());
        activoExistente.setProveedor(activoActualizado.getProveedor());

        return activoRepository.save(activoExistente);
    }

    @Transactional
    public void eliminar(Long id) {
        Activo activo = activoRepository.findById(id)
            .orElseThrow(() -> new ActivoNoEncontradoException("Activo con ID " + id + " no encontrado"));

        if (!"Dado de baja".equalsIgnoreCase(activo.getEstado().getEstadoNom())) {
            throw new ActivoConDependenciasException("Solo se pueden eliminar activos en estado 'Dado de baja'");
        }

        activoRepository.delete(activo);
    }

    @Transactional
    public void cambiarEstado(Long activoId, Long nuevoEstadoId, String motivo,
                             String observaciones, Usuario usuario) {
        estadoTransicionService.cambiarEstado(activoId, nuevoEstadoId, motivo, observaciones, usuario);
    }

    public Page<Activo> buscarConFiltros(String buscar, Long categoriaId, Long estadoId,
                                         Long departamentoId, Pageable pageable) {
        return activoRepository.findByFiltros(buscar, categoriaId, estadoId, departamentoId, pageable);
    }

    public Page<Activo> buscarPorNombre(String nombre, Pageable pageable) {
        return activoRepository.findByActivoNomContainingIgnoreCase(nombre, pageable);
    }

    public Page<Activo> buscarPorCategoria(Long categoriaId, Pageable pageable) {
        return activoRepository.findByCategoria_CatId(categoriaId, pageable);
    }

    public Page<Activo> buscarPorEstado(Long estadoId, Pageable pageable) {
        return activoRepository.findByEstado_EstadoId(estadoId, pageable);
    }

    public List<Activo> buscarPorEstado(EstadoActivo estado) {
        return activoRepository.findByEstado(estado);
    }

    public List<Activo> buscarPorCategoria(CategoriasActivo categoria) {
        return activoRepository.findByCategoria(categoria);
    }

    public long contarPorEstado(EstadoActivo estado) {
        return activoRepository.countByEstado(estado);
    }

    public long contarPorCategoria(CategoriasActivo categoria) {
        return activoRepository.countByCategoria(categoria);
    }

    public long contarPorDepartamento(Departamentos departamento) {
        return activoRepository.countByDepartamentoId(departamento.getDeptId());
    }

    public long contarTodos() {
        return activoRepository.count();
    }

    public List<Object[]> contarPorEstado() {
        return activoRepository.countActivosPorEstado();
    }

    public List<Object[]> contarPorCategoria() {
        Map<String, Long> acumulado = new LinkedHashMap<>();
        for (Object[] fila : activoRepository.countActivosPorCategoria()) {
            String categoriaNormalizada = normalizarCategoriaPrincipal(String.valueOf(fila[0]));
            long cantidad = ((Number) fila[1]).longValue();
            acumulado.merge(categoriaNormalizada, cantidad, Long::sum);
        }

        return acumulado.entrySet().stream()
            .map(entry -> new Object[] { entry.getKey(), entry.getValue() })
            .toList();
    }

    public List<Object[]> contarPorDepartamento() {
        return activoRepository.countActivosPorDepartamento();
    }

    public List<EstadoActivo> obtenerEstadosPorCategoria(Long categoriaId) {
        return obtenerTodosEstados();
    }

    public boolean esEstadoValidoParaCategoria(Long categoriaId, Long estadoId) {
        return true;
    }

    private EstadoActivo obtenerEstadoInicial() {
        return estadoActivoRepository.findByEstadoNom("Disponible")
            .or(() -> estadoActivoRepository.findByEstadoNom("Activo"))
            .orElseThrow(() -> new ActivoInvalidoException("No existe estado inicial 'Disponible'"));
    }

    private void validarActivo(Activo activo, Long idActual) {
        String codigoNormalizado = normalizarCodigo(activo.getActivoCodigo());
        if (codigoNormalizado == null || codigoNormalizado.isEmpty()) {
            throw new ActivoInvalidoException("El codigo del activo es obligatorio");
        }

        if (contieneEspacios(activo.getActivoCodigo())) {
            throw new ActivoInvalidoException("El codigo del activo no puede contener espacios");
        }

        Optional<Activo> activoConCodigo = activoRepository.findByActivoCodigoIgnoreCase(codigoNormalizado);
        if (activoConCodigo.isPresent() && !activoConCodigo.get().getActivoId().equals(idActual)) {
            throw new ActivoInvalidoException("Ya existe un activo con el codigo: " + codigoNormalizado);
        }
        if (activo.getActivoNom() == null || activo.getActivoNom().trim().isEmpty()) {
            throw new ActivoInvalidoException("El nombre del activo es obligatorio");
        }
        if (activo.getCategoria() == null) {
            throw new ActivoInvalidoException("La categoria es obligatoria");
        }
    }

    private void validarActualizacion(Activo activo) {
        if (activo.getActivoNom() == null || activo.getActivoNom().trim().isEmpty()) {
            throw new ActivoInvalidoException("El nombre del activo es obligatorio");
        }
    }

    public List<CategoriasActivo> obtenerTodasCategorias() {
        return categoriasRepository.findByCatActivoTrueOrderByCatNomAsc();
    }

    public List<EstadoActivo> obtenerTodosEstados() {
        return estadoActivoRepository.findAllByOrderByEstadoNomAsc();
    }

    public List<EstadoActivo> obtenerEstadosInicialesPermitidos() {
        return estadoActivoRepository.findAllByOrderByEstadoNomAsc().stream()
            .filter(estado -> "Disponible".equalsIgnoreCase(estado.getEstadoNom())
                || "Asignado".equalsIgnoreCase(estado.getEstadoNom()))
            .toList();
    }

    public List<Departamentos> obtenerTodosDepartamentos() {
        return departamentosRepository.findAll();
    }

    public List<Proveedores> obtenerTodosProveedores() {
        return proveedoresRepository.findAll();
    }

    public List<Marca> obtenerTodasMarcas() {
        return marcaRepository.findAllByOrderByMarcaNomAsc();
    }

    public List<Modelo> obtenerTodosModelos() {
        return modeloRepository.findAllByOrderByModelNomAsc();
    }

    public List<Modelo> obtenerModelosPorMarca(Long marcaId) {
        if (marcaId == null) {
            return obtenerTodosModelos();
        }

        return modeloRepository.findByMarca_MarcaIdOrderByModelNomAsc(marcaId);
    }

    private String normalizarCategoriaPrincipal(String categoria) {
        if (categoria == null || categoria.isBlank()) {
            return "Otro";
        }

        return switch (categoria) {
            case "Notebook", "Laptop" -> "Laptop";
            case "PC Escritorio", "Computadora de Escritorio" -> "Computadora de Escritorio";
            case "Router", "Switch", "Access Point", "Firewall", "Dispositivo de Red" -> "Dispositivo de Red";
            case "Monitor", "Periférico" -> "Periférico";
            case "Impresora", "Impresora / Multifunción" -> "Impresora / Multifunción";
            case "UPS", "Equipo de Energía" -> "Equipo de Energía";
            case "Servidor" -> "Servidor";
            case "Componente" -> "Componente";
            case "Dispositivo IoT" -> "Dispositivo IoT";
            case "Dispositivo Móvil" -> "Dispositivo Móvil";
            case "Otro" -> "Otro";
            default -> categoria;
        };
    }

    private String normalizarCodigo(String activoCodigo) {
        if (activoCodigo == null) {
            return null;
        }

        return activoCodigo.trim().toUpperCase();
    }

    private boolean contieneEspacios(String activoCodigo) {
        return activoCodigo != null && activoCodigo.chars().anyMatch(Character::isWhitespace);
    }

    private HardwareInfo crearHardwareParaActivo(Activo activo, Long marcaId, Long modeloId, String numeroSerie) {
        if (marcaId == null || modeloId == null || numeroSerie == null || numeroSerie.trim().isEmpty()) {
            throw new ActivoInvalidoException("Marca, modelo y numero de serie son obligatorios para un activo fisico");
        }

        Marca marca = marcaRepository.findById(marcaId)
            .orElseThrow(() -> new ActivoInvalidoException("Marca no encontrada"));
        Modelo modelo = modeloRepository.findById(modeloId)
            .orElseThrow(() -> new ActivoInvalidoException("Modelo no encontrado"));

        if (modelo.getMarca() == null || modelo.getMarca().getMarcaId() == null
                || !modelo.getMarca().getMarcaId().equals(marca.getMarcaId())) {
            throw new ActivoInvalidoException("El modelo seleccionado no pertenece a la marca indicada");
        }

        String serialNormalizado = numeroSerie.trim();
        if (hardwareInfoService.existeSerial(serialNormalizado)) {
            throw new ActivoInvalidoException("Ya existe un hardware con el numero de serie: " + serialNormalizado);
        }

        HardwareInfo hardware = new HardwareInfo();
        hardware.setActivo(activo);
        hardware.setModelo(modelo);
        hardware.setHwSerialNum(serialNormalizado);
        hardware.setHwDescri(null);
        return hardware;
    }
}
