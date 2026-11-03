package com.banco.controller;

import com.banco.model.Rol;
import com.banco.patterns.command.InvocadorCaja;
import com.banco.patterns.proxy.ArchivoReporte;
import com.banco.patterns.proxy.ServicioReportes;
import com.banco.patterns.proxy.TipoReporte;
import com.banco.service.AuditoriaService;
import com.banco.service.CuentaService;
import com.banco.service.ParametroService;
import com.banco.service.UsuarioService;
import com.banco.util.DatabaseException;
import com.banco.util.NegocioException;
import com.banco.util.RolActual;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

/**
 * GRASP CONTROLADOR (actor ADMINISTRADOR). Sus cuatro funciones:
 *  1. Gestionar usuarios y roles (crear, activar/desactivar, cambiar rol, desbloquear, restablecer clave).
 *  2. Configurar parametros, con historial y restauracion (patron Memento).
 *  3. Reportes globales con exportacion (patrones Bridge y Proxy). Idea tomada de BBVA.
 *  4. Auditar operaciones de caja y extornarlas (patron Command).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final List<Rol> ROLES_PERSONAL = List.of(Rol.ADMINISTRADOR, Rol.ASESOR, Rol.CAJERO, Rol.AUDITOR);
    private static final int DIAS_GRAFICO = 7;

    private final UsuarioService usuarioService;
    private final ParametroService parametroService;
    private final AuditoriaService auditoriaService;
    private final ServicioReportes reportes;
    private final InvocadorCaja invocadorCaja;
    private final CuentaService cuentaService;

    public AdminController(UsuarioService usuarioService, ParametroService parametroService,
                           AuditoriaService auditoriaService, ServicioReportes reportes, InvocadorCaja invocadorCaja,
                           CuentaService cuentaService) {
        this.cuentaService = cuentaService;
        this.usuarioService = usuarioService;
        this.parametroService = parametroService;
        this.auditoriaService = auditoriaService;
        this.reportes = reportes;
        this.invocadorCaja = invocadorCaja;
    }

    @GetMapping("")
    public String inicio() {
        return "redirect:/admin/reportes";
    }

    // ----------------------------- Usuarios y roles -----------------------------

    @GetMapping("/usuarios")
    public String usuarios(Model model) {
        cargarUsuarios(model);
        return "admin/usuarios";
    }

    @PostMapping("/usuarios")
    public String crearUsuario(@RequestParam String username, @RequestParam String password,
                               @RequestParam String nombre, @RequestParam Rol rol,
                               Model model, RedirectAttributes flash) {
        try {
            usuarioService.crearPersonal(username, password, nombre, rol, true);
            flash.addFlashAttribute("ok", "Usuario " + username + " creado. La clave que pusiste es temporal: "
                    + "el sistema le pedira cambiarla en su primer ingreso");
            return "redirect:/admin/usuarios";
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            cargarUsuarios(model);
            return "admin/usuarios";
        }
    }

    @PostMapping("/usuarios/{id}/estado")
    public String cambiarEstado(@PathVariable int id, @RequestParam boolean activo,
                                Principal principal, RedirectAttributes flash) {
        try {
            usuarioService.cambiarEstado(id, activo, principal.getName());
            flash.addFlashAttribute("ok", activo ? "Usuario activado" : "Usuario desactivado");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/rol")
    public String cambiarRol(@PathVariable int id, @RequestParam Rol rol,
                             Principal principal, RedirectAttributes flash) {
        try {
            usuarioService.cambiarRol(id, rol, principal.getName());
            flash.addFlashAttribute("ok", "Rol actualizado");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/desbloquear")
    public String desbloquear(@PathVariable int id, Principal principal, RedirectAttributes flash) {
        try {
            usuarioService.desbloquear(id, principal.getName());
            flash.addFlashAttribute("ok", "Usuario desbloqueado");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/clave")
    public String restablecerClave(@PathVariable int id, Authentication auth, RedirectAttributes flash) {
        try {
            String temporal = usuarioService.restablecerClave(id, auth.getName(), RolActual.de(auth));
            flash.addFlashAttribute("claveTemporal", temporal);
            flash.addFlashAttribute("ok", "Clave restablecida. Entregale la clave temporal al usuario: "
                    + "debera cambiarla al ingresar");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/usuarios";
    }

    // ----------------------------- Parametros (patron Memento) -----------------------------

    @GetMapping("/parametros")
    public String parametros(Model model) {
        model.addAttribute("parametros", parametroService.listar());
        model.addAttribute("versiones", parametroService.versiones());
        return "admin/parametros";
    }

    @PostMapping("/parametros")
    public String guardarParametro(@RequestParam String clave, @RequestParam String valor,
                                   Principal principal, RedirectAttributes flash) {
        try {
            parametroService.actualizar(clave, valor, principal.getName());
            flash.addFlashAttribute("ok", "Parametro actualizado. La version anterior quedo guardada en el historial");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/parametros";
    }

    @PostMapping("/parametros/restaurar/{versionId}")
    public String restaurar(@PathVariable int versionId, Principal principal, RedirectAttributes flash) {
        try {
            parametroService.restaurar(versionId, principal.getName());
            flash.addFlashAttribute("ok", "Se restauro la version #" + versionId
                    + ". La configuracion que habia antes tambien quedo guardada");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/parametros";
    }

    // ----------------------------- Reportes globales (Bridge + Proxy) -----------------------------

    @GetMapping("/reportes")
    public String reportes(Model model) {
        List<Object[]> porTipo = auditoriaService.resumenPorTipo();
        List<Object[]> porDia = auditoriaService.resumenPorDia(DIAS_GRAFICO);
        BigDecimal total = BigDecimal.ZERO;
        int cantidad = 0;
        for (Object[] f : porTipo) {
            if ("TRANSFERENCIA_RECIBIDA".equals(f[0])) {
                continue; // una transferencia es UNA operacion: ya se conto como TRANSFERENCIA_ENVIADA
            }
            total = total.add((BigDecimal) f[2]);
            cantidad += (Integer) f[1];
        }
        BigDecimal maximoDia = porDia.stream().map(f -> (BigDecimal) f[2]).reduce(BigDecimal.ONE, BigDecimal::max);
        BigDecimal maximoTipo = porTipo.stream().map(f -> (BigDecimal) f[2]).reduce(BigDecimal.ONE, BigDecimal::max);
        model.addAttribute("porTipo", porTipo);
        model.addAttribute("porDia", porDia);
        model.addAttribute("maximoDia", maximoDia);
        model.addAttribute("maximoTipo", maximoTipo);
        model.addAttribute("montoTotal", total);
        model.addAttribute("cantidadTotal", cantidad);
        model.addAttribute("usuarios", usuarioService.listar().size());
        model.addAttribute("alertas", auditoriaService.alertasPendientes());
        model.addAttribute("recaudado", cuentaService.saldoRecaudado());
        model.addAttribute("porServicio", auditoriaService.recaudadoPorServicio());
        return "admin/reportes";
    }

    @GetMapping("/reportes/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam(defaultValue = "pdf") String formato, Authentication auth) {
        ArchivoReporte archivo = reportes.generar(TipoReporte.GLOBAL, formato, auth.getName(), RolActual.de(auth));
        return Descargas.archivo(archivo);
    }

    // ----------------------------- Auditar operaciones (Command) -----------------------------

    @GetMapping("/operaciones")
    public String operaciones(Model model) {
        model.addAttribute("operaciones", invocadorCaja.historial());
        return "admin/operaciones";
    }

    @PostMapping("/operaciones/{id}/extornar")
    public String extornar(@PathVariable int id, @RequestParam String motivo,
                           Principal principal, RedirectAttributes flash) {
        try {
            invocadorCaja.extornar(id, principal.getName(), motivo);
            flash.addFlashAttribute("ok", "Operacion #" + id + " extornada: se registro el movimiento inverso");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/operaciones";
    }

    private void cargarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("roles", ROLES_PERSONAL);
    }
}
