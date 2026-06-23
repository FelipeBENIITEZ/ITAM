package com.sistema.iTsystem.service;

import java.time.LocalDateTime;
import java.util.List;
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
import com.sistema.iTsystem.model.Proveedores;
import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.ActivoRepository;
import com.sistema.iTsystem.repository.CategoriasActivoRepository;
import com.sistema.iTsystem.repository.DepartamentosRepository;
import com.sistema.iTsystem.repository.EstadoActivoRepository;
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

    @Transactional
    public Activo crear(Activo activo) {
        validarActivo(activo, null);

        if (activo.getEstado() == null) {
            activo.setEstado(obtenerEstadoInicial());
        }
        if (activo.getActivoFechaIngreso() == null) {
            activo.setActivoFechaIngreso(LocalDateTime.now());
        }
        if (activo.getActivoActivo() == null) {
            activo.setActivoActivo(true);
        }

        return activoRepository.save(activo);
    }

    @Transactional
    public Activo actualizar(Long id, Activo activoActualizado) {
        Activo activoExistente = activoRepository.findById(id)
            .orElseThrow(() -> new ActivoNoEncontradoException("Activo con ID " + id + " no encontrado"));

        validarActivo(activoActualizado, id);

        activoExistente.setActivoCodigo(activoActualizado.getActivoCodigo());
        activoExistente.setActivoNom(activoActualizado.getActivoNom());
        activoExistente.setActivoDescri(activoActualizado.getActivoDescri());
        activoExistente.setActivoFechaIngreso(activoActualizado.getActivoFechaIngreso());
        activoExistente.setActivoFechaEgreso(activoActualizado.getActivoFechaEgreso());
        activoExistente.setActivoActivo(activoActualizado.getActivoActivo());
        activoExistente.setProveedor(activoActualizado.getProveedor());
        activoExistente.setCategoria(activoActualizado.getCategoria());

        if (activoActualizado.getEstado() != null) {
            activoExistente.setEstado(activoActualizado.getEstado());
        }

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
        return activoRepository.countActivosPorCategoria();
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
        if (activo.getActivoCodigo() == null || activo.getActivoCodigo().trim().isEmpty()) {
            throw new ActivoInvalidoException("El codigo del activo es obligatorio");
        }
        Optional<Activo> activoConCodigo = activoRepository.findByActivoCodigo(activo.getActivoCodigo());
        if (activoConCodigo.isPresent() && !activoConCodigo.get().getActivoId().equals(idActual)) {
            throw new ActivoInvalidoException("Ya existe un activo con el codigo: " + activo.getActivoCodigo());
        }
        if (activo.getActivoNom() == null || activo.getActivoNom().trim().isEmpty()) {
            throw new ActivoInvalidoException("El nombre del activo es obligatorio");
        }
        if (activo.getCategoria() == null) {
            throw new ActivoInvalidoException("La categoria es obligatoria");
        }
    }

    public List<CategoriasActivo> obtenerTodasCategorias() {
        return categoriasRepository.findAllByOrderByCatNomAsc();
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
}
