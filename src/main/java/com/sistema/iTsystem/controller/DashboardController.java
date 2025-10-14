package com.sistema.iTsystem.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sistema.iTsystem.model.Usuario;
import com.sistema.iTsystem.repository.UsuarioRepository;
import com.sistema.iTsystem.service.ActivoService;
import com.sistema.iTsystem.service.ContratoInfoService;
import com.sistema.iTsystem.service.EventosService;
import com.sistema.iTsystem.service.HardwareInfoService;
import com.sistema.iTsystem.service.LicenciaInfoService;
import com.sistema.iTsystem.service.MantenimientoService;
import com.sistema.iTsystem.service.SoftwareInfoService;
import com.sistema.iTsystem.service.SolicitudesService;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    // ==================== SERVICES ====================
    
    @Autowired
    private ActivoService activoService;
    
    @Autowired
    private HardwareInfoService hardwareService;
    
    @Autowired
    private SoftwareInfoService softwareService;
    
    @Autowired
    private LicenciaInfoService licenciaService;
    
    @Autowired
    private ContratoInfoService contratoService;
    
    @Autowired
    private MantenimientoService mantenimientoService;
    
    @Autowired
    private SolicitudesService solicitudesService;
    
    @Autowired
    private EventosService eventosService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

    // ==================== DASHBOARD PRINCIPAL ====================
    
    @GetMapping
    public String dashboard(Principal principal, Model model) {
        try {
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            boolean esAdmin = usuario.getRol().getRolId().equals(1L);
            
            model.addAttribute("usuario", usuario);
            model.addAttribute("esAdmin", esAdmin);
            
            // ==================== MÉTRICAS GENERALES ====================
            
            // Activos
            model.addAttribute("totalActivos", activoService.obtenerTodos().size());
            
            // Hardware
            model.addAttribute("totalHardware", hardwareService.obtenerTodos().size());
            model.addAttribute("hardwareDisponible", hardwareService.buscarDisponibles().size());
            model.addAttribute("hardwareEnUso", hardwareService.buscarEnUso().size());
            model.addAttribute("hardwareEnMantenimiento", hardwareService.buscarEnMantenimiento().size());
            model.addAttribute("hardwareProyectado", hardwareService.buscarProyectados().size());
            model.addAttribute("hardwareFueraServicio", hardwareService.buscarFueraDeServicio().size());
            
            // Software
            model.addAttribute("totalSoftware", softwareService.obtenerTodos().size());
            
            // Licencias
            model.addAttribute("totalLicencias", licenciaService.obtenerTodas().size());
            model.addAttribute("licenciasActivas", licenciaService.buscarActivas().size());
            model.addAttribute("licenciasVencidas", licenciaService.contarVencidas());
            model.addAttribute("licenciasProximasVencer", licenciaService.contarProximasAVencer());
            model.addAttribute("cuposDisponibles", licenciaService.contarCuposDisponibles());
            
            // Contratos
            model.addAttribute("totalContratos", contratoService.obtenerTodos().size());
            model.addAttribute("contratosVigentes", contratoService.contarVigentes());
            model.addAttribute("contratosVencidos", contratoService.contarVencidos());
            model.addAttribute("contratosProximosVencer", contratoService.contarProximosAVencer());
            
            // Mantenimientos
            model.addAttribute("totalMantenimientos", mantenimientoService.obtenerTodos().size());
            model.addAttribute("mantenimientosEnCurso", mantenimientoService.contarEnCurso());
            model.addAttribute("costoTotalMantenimientos", mantenimientoService.calcularCostoTotal());
            model.addAttribute("costoPromedioMantenimiento", mantenimientoService.calcularCostoPromedio());
            
            // Solicitudes
            if (esAdmin) {
                model.addAttribute("totalSolicitudes", solicitudesService.obtenerTodas().size());
                model.addAttribute("solicitudesPendientes", solicitudesService.contarPendientes());
            } else {
                model.addAttribute("totalSolicitudes", 
                    solicitudesService.buscarPorUsuario(usuario.getUsuId()).size());
                
                // Contar pendientes del usuario manualmente
                long pendientesUsuario = solicitudesService.buscarPorUsuario(usuario.getUsuId()).stream()
                    .filter(s -> s.estaPendiente())
                    .count();
                model.addAttribute("solicitudesPendientes", pendientesUsuario);
            }
            
            // Eventos
            model.addAttribute("totalEventos", eventosService.obtenerTodos().size());
            model.addAttribute("eventosCriticos", eventosService.contarCriticos());
            model.addAttribute("eventosHoy", eventosService.buscarDeHoy().size());
            
            // ==================== COSTOS Y FINANCIERO ====================
            
            BigDecimal costoLicencias = licenciaService.calcularCostoTotal();
            BigDecimal costoMantenimientos = mantenimientoService.calcularCostoTotal();
            BigDecimal valorHardware = hardwareService.calcularValorTotal();
            BigDecimal costoTotal = costoLicencias.add(costoMantenimientos).add(valorHardware);
            
            model.addAttribute("costoTotalLicencias", costoLicencias);
            model.addAttribute("costoTotalMantenimientos", costoMantenimientos);
            model.addAttribute("valorTotalHardware", valorHardware);
            model.addAttribute("costoTotalGeneral", costoTotal);
            
            // ==================== ALERTAS ====================
            
            // Licencias
            model.addAttribute("alertaLicenciasVencidas", licenciaService.buscarVencidas());
            model.addAttribute("alertaLicenciasProximasVencer", licenciaService.buscarProximasAVencer());
            model.addAttribute("alertaLicenciasSinCupos", licenciaService.buscarSinCupos());
            
            // Contratos
            model.addAttribute("alertaContratosVencidos", contratoService.buscarVencidos());
            model.addAttribute("alertaContratosProximosVencer", contratoService.buscarProximosAVencer());
            
            // Hardware
            model.addAttribute("alertaHardwareSinValor", hardwareService.buscarSinValorCompra());
            model.addAttribute("alertaHardwareSinProveedor", hardwareService.buscarSinProveedor());
            
            // Eventos críticos
            model.addAttribute("alertaEventosCriticos", eventosService.buscarCriticos());
            
            // ==================== GRÁFICOS Y ESTADÍSTICAS ====================
            
            // Activos por categoría
            model.addAttribute("activosPorCategoria", activoService.contarPorCategoria());
            
            // Activos por estado
            model.addAttribute("activosPorEstado", activoService.contarPorEstado());
            
            // Activos por departamento
            model.addAttribute("activosPorDepartamento", activoService.contarPorDepartamento());
            
            // Hardware por marca (a través del modelo)
            model.addAttribute("hardwarePorMarca", hardwareService.contarPorTipo());
            
            // Hardware por modelo
            model.addAttribute("hardwarePorModelo", hardwareService.contarPorModelo());
            
            // Hardware por estado
            model.addAttribute("hardwarePorEstado", hardwareService.contarPorEstado());
            
            // Hardware por proveedor
            model.addAttribute("hardwarePorProveedor", hardwareService.contarPorProveedor());
            
            // Software por tipo
            model.addAttribute("softwarePorTipo", softwareService.contarPorTipo());
            
            // Licencias por estado
            model.addAttribute("licenciasPorEstado", licenciaService.contarPorEstado());
            
            // Mantenimientos por tipo
            model.addAttribute("mantenimientosPorTipo", mantenimientoService.contarPorTipo());
            
            // Mantenimientos por mes
            model.addAttribute("mantenimientosPorMes", mantenimientoService.contarPorMes());
            
            // Eventos por nivel
            model.addAttribute("eventosPorNivel", eventosService.contarPorNivel());
            
            // Eventos por mes
            model.addAttribute("eventosPorMes", eventosService.contarPorMes());
            
            if (esAdmin) {
                // Solicitudes por estado
                model.addAttribute("solicitudesPorEstado", solicitudesService.contarPorEstado());
                
                // Solicitudes por tipo
                model.addAttribute("solicitudesPorTipo", solicitudesService.contarPorTipo());
            }
            
            // ==================== ACTIVIDAD RECIENTE ====================
            
            model.addAttribute("ultimosMantenimientos", mantenimientoService.obtenerUltimos(5));
            model.addAttribute("ultimosEventos", eventosService.obtenerUltimos(10));
            model.addAttribute("ultimasSolicitudes", solicitudesService.obtenerUltimas(5));
            
            // ==================== PRÓXIMOS VENCIMIENTOS ====================
            
            model.addAttribute("proximosVencimientosLicencias", 
                licenciaService.buscarProximasAVencer().stream().limit(5).toList());
            model.addAttribute("proximosVencimientosContratos", 
                contratoService.buscarProximosAVencer().stream().limit(5).toList());
            
            return "dashboard/index";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar dashboard: " + e.getMessage());
            return "dashboard/index";
        }
    }

    // ==================== DASHBOARD DE ACTIVOS ====================
    
    @GetMapping("/activos")
    public String dashboardActivos(Model model) {
        try {
            model.addAttribute("totalActivos", activoService.obtenerTodos().size());
            model.addAttribute("activosPorCategoria", activoService.contarPorCategoria());
            model.addAttribute("activosPorEstado", activoService.contarPorEstado());
            model.addAttribute("activosPorDepartamento", activoService.contarPorDepartamento());
            
            return "dashboard/activos";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD DE HARDWARE ====================
    
    @GetMapping("/hardware")
    public String dashboardHardware(Model model) {
        try {
            model.addAttribute("totalHardware", hardwareService.obtenerTodos().size());
            model.addAttribute("disponibles", hardwareService.buscarDisponibles().size());
            model.addAttribute("enUso", hardwareService.buscarEnUso().size());
            model.addAttribute("enMantenimiento", hardwareService.buscarEnMantenimiento().size());
            model.addAttribute("proyectados", hardwareService.buscarProyectados().size());
            model.addAttribute("fueraServicio", hardwareService.buscarFueraDeServicio().size());
            model.addAttribute("dadosBaja", hardwareService.buscarDadosDeBaja().size());
            
            model.addAttribute("valorTotal", hardwareService.calcularValorTotal());
            model.addAttribute("valorPromedio", hardwareService.calcularValorPromedio());
            model.addAttribute("sinValorCompra", hardwareService.contarSinValorCompra());
            model.addAttribute("sinProveedor", hardwareService.buscarSinProveedor().size());
            
            // Estadísticas
            model.addAttribute("hardwarePorMarca", hardwareService.contarPorTipo());
            model.addAttribute("hardwarePorModelo", hardwareService.contarPorModelo());
            model.addAttribute("hardwarePorEstado", hardwareService.contarPorEstado());
            model.addAttribute("hardwarePorProveedor", hardwareService.contarPorProveedor());
            
            // Alertas
            model.addAttribute("hardwareSinValor", hardwareService.buscarSinValorCompra());
            model.addAttribute("hardwareSinProveedor", hardwareService.buscarSinProveedor());
            
            return "dashboard/hardware";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD DE SOFTWARE Y LICENCIAS ====================
    
    @GetMapping("/software-licencias")
    public String dashboardSoftwareLicencias(Model model) {
        try {
            // Software
            model.addAttribute("totalSoftware", softwareService.obtenerTodos().size());
            model.addAttribute("softwarePorTipo", softwareService.contarPorTipo());
            model.addAttribute("softwarePorProveedor", softwareService.contarPorProveedor());
            
            // Licencias
            model.addAttribute("totalLicencias", licenciaService.obtenerTodas().size());
            model.addAttribute("licenciasActivas", licenciaService.buscarActivas().size());
            model.addAttribute("licenciasVencidas", licenciaService.contarVencidas());
            model.addAttribute("licenciasProximasVencer", licenciaService.contarProximasAVencer());
            model.addAttribute("licenciasPorEstado", licenciaService.contarPorEstado());
            model.addAttribute("costoTotalLicencias", licenciaService.calcularCostoTotal());
            model.addAttribute("cuposDisponibles", licenciaService.contarCuposDisponibles());
            
            // Alertas
            model.addAttribute("alertaLicenciasVencidas", 
                licenciaService.buscarVencidas().stream().limit(10).toList());
            model.addAttribute("alertaLicenciasProximasVencer", 
                licenciaService.buscarProximasAVencer().stream().limit(10).toList());
            model.addAttribute("alertaLicenciasSinCupos", 
                licenciaService.buscarSinCupos().stream().limit(10).toList());
            
            return "dashboard/software-licencias";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD DE CONTRATOS ====================
    
    @GetMapping("/contratos")
    public String dashboardContratos(Model model) {
        try {
            model.addAttribute("totalContratos", contratoService.obtenerTodos().size());
            model.addAttribute("contratosVigentes", contratoService.contarVigentes());
            model.addAttribute("contratosVencidos", contratoService.contarVencidos());
            model.addAttribute("contratosProximosVencer", contratoService.contarProximosAVencer());
            model.addAttribute("contratosConArchivo", contratoService.contarConArchivo());
            model.addAttribute("contratosSinArchivo", contratoService.contarSinArchivo());
            
            // Alertas
            model.addAttribute("alertaContratosVencidos", 
                contratoService.buscarVencidos().stream().limit(10).toList());
            model.addAttribute("alertaContratosProximosVencer", 
                contratoService.buscarProximosAVencer().stream().limit(10).toList());
            
            return "dashboard/contratos";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD DE MANTENIMIENTOS ====================
    
    @GetMapping("/mantenimientos")
    public String dashboardMantenimientos(Model model) {
        try {
            model.addAttribute("totalMantenimientos", mantenimientoService.obtenerTodos().size());
            model.addAttribute("enCurso", mantenimientoService.contarEnCurso());
            model.addAttribute("mantenimientosPorTipo", mantenimientoService.contarPorTipo());
            model.addAttribute("mantenimientosPorMes", mantenimientoService.contarPorMes());
            model.addAttribute("costoTotal", mantenimientoService.calcularCostoTotal());
            model.addAttribute("costoPromedio", mantenimientoService.calcularCostoPromedio());
            model.addAttribute("sinCosto", mantenimientoService.contarSinCosto());
            
            // Mantenimientos recientes
            model.addAttribute("mantenimientosEnCurso", mantenimientoService.buscarEnCurso());
            model.addAttribute("ultimosMantenimientos", mantenimientoService.obtenerUltimos(10));
            model.addAttribute("masCostosos", mantenimientoService.buscarMasCostosos());
            
            return "dashboard/mantenimientos";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD DE SOLICITUDES ====================
    
    @GetMapping("/solicitudes")
    public String dashboardSolicitudes(Principal principal, Model model) {
        try {
            Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            boolean esAdmin = usuario.getRol().getRolId().equals(1L);
            
            if (!esAdmin) {
                // Usuario normal solo ve sus estadísticas
                java.util.List<com.sistema.iTsystem.model.Solicitudes> misSolicitudes = 
                    solicitudesService.buscarPorUsuario(usuario.getUsuId());
                
                model.addAttribute("totalSolicitudes", misSolicitudes.size());
                model.addAttribute("misSolicitudesPendientes", 
                    misSolicitudes.stream().filter(s -> s.estaPendiente()).toList());
                model.addAttribute("misSolicitudesAprobadas", 
                    misSolicitudes.stream().filter(s -> s.estaAprobada()).toList());
                model.addAttribute("misSolicitudesRechazadas", 
                    misSolicitudes.stream().filter(s -> s.estaRechazada()).toList());
                
                return "dashboard/mis-solicitudes";
            }
            
            // Admin ve todo
            model.addAttribute("totalSolicitudes", solicitudesService.obtenerTodas().size());
            model.addAttribute("pendientes", solicitudesService.contarPendientes());
            model.addAttribute("solicitudesPorEstado", solicitudesService.contarPorEstado());
            model.addAttribute("solicitudesPorTipo", solicitudesService.contarPorTipo());
            model.addAttribute("solicitudesPorUsuario", solicitudesService.contarPorUsuario());
            
            // Listas
            model.addAttribute("solicitudesPendientes", solicitudesService.buscarPendientes());
            model.addAttribute("ultimasSolicitudes", solicitudesService.obtenerUltimas(10));
            
            model.addAttribute("esAdmin", true);
            
            return "dashboard/solicitudes";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD DE EVENTOS ====================
    
    @GetMapping("/eventos")
    public String dashboardEventos(Model model) {
        try {
            model.addAttribute("totalEventos", eventosService.obtenerTodos().size());
            model.addAttribute("eventosCriticos", eventosService.contarCriticos());
            model.addAttribute("eventosPorNivel", eventosService.contarPorNivel());
            model.addAttribute("eventosPorMes", eventosService.contarPorMes());
            
            // Eventos recientes y críticos
            model.addAttribute("eventosDeHoy", eventosService.buscarDeHoy());
            model.addAttribute("eventosCriticosLista", eventosService.buscarCriticos());
            model.addAttribute("ultimosEventos", eventosService.obtenerUltimos(20));
            
            return "dashboard/eventos";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD FINANCIERO ====================
    
    @GetMapping("/financiero")
    public String dashboardFinanciero(Principal principal, Model model) {
        try {
            validarEsAdministrador(principal);
            
            BigDecimal costoLicencias = licenciaService.calcularCostoTotal();
            BigDecimal costoMantenimientos = mantenimientoService.calcularCostoTotal();
            BigDecimal valorHardware = hardwareService.calcularValorTotal();
            BigDecimal costoTotal = costoLicencias.add(costoMantenimientos).add(valorHardware);
            
            model.addAttribute("costoTotalLicencias", costoLicencias);
            model.addAttribute("costoTotalMantenimientos", costoMantenimientos);
            model.addAttribute("valorTotalHardware", valorHardware);
            model.addAttribute("costoTotalGeneral", costoTotal);
            model.addAttribute("costoPromedioMantenimiento", mantenimientoService.calcularCostoPromedio());
            model.addAttribute("valorPromedioHardware", hardwareService.calcularValorPromedio());
            
            // Detalles
            model.addAttribute("licenciasMasCostosas", 
                licenciaService.obtenerTodas().stream()
                    .sorted((a, b) -> b.getLicenciaCosto().compareTo(a.getLicenciaCosto()))
                    .limit(10)
                    .toList());
            
            model.addAttribute("mantenimientosMasCostosos", mantenimientoService.buscarMasCostosos());
            
            return "dashboard/financiero";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== DASHBOARD DE ALERTAS ====================
    
    @GetMapping("/alertas")
    public String dashboardAlertas(Model model) {
        try {
            // Licencias
            model.addAttribute("licenciasVencidas", licenciaService.buscarVencidas());
            model.addAttribute("licenciasProximasVencer", licenciaService.buscarProximasAVencer());
            model.addAttribute("licenciasSinCupos", licenciaService.buscarSinCupos());
            
            // Contratos
            model.addAttribute("contratosVencidos", contratoService.buscarVencidos());
            model.addAttribute("contratosProximosVencer", contratoService.buscarProximosAVencer());
            model.addAttribute("contratosSinArchivo", contratoService.buscarSinArchivo());
            
            // Hardware
            model.addAttribute("hardwareSinValor", hardwareService.buscarSinValorCompra());
            model.addAttribute("hardwareSinProveedor", hardwareService.buscarSinProveedor());
            model.addAttribute("hardwareFueraServicio", hardwareService.buscarFueraDeServicio());
            
            // Eventos
            model.addAttribute("eventosCriticos", eventosService.buscarCriticos());
            
            // Mantenimientos
            model.addAttribute("mantenimientosEnCurso", mantenimientoService.buscarEnCurso());
            
            return "dashboard/alertas";
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // ==================== MÉTODOS HELPER ====================
    
    /**
     * Validar que el usuario sea administrador (rol_id = 1)
     */
    private void validarEsAdministrador(Principal principal) {
        Usuario usuario = usuarioRepository.findByUsuLogin(principal.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!usuario.getRol().getRolId().equals(1L)) {
            throw new RuntimeException("Esta acción requiere privilegios de Administrador");
        }
    }
}
