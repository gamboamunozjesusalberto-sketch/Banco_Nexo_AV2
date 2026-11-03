package com.banco.controller;

import com.banco.model.Cliente;
import com.banco.model.Cuenta;
import com.banco.model.TipoCuenta;
import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;
import com.banco.model.Usuario;
import com.banco.patterns.command.ComandoCaja;
import com.banco.patterns.command.InvocadorCaja;
import com.banco.service.UsuarioService;
import com.banco.util.RolActual;
import org.springframework.security.core.Authentication;
import java.security.Principal;
import com.banco.service.ClienteService;
import com.banco.service.CuentaService;
import com.banco.util.DatabaseException;
import com.banco.util.NegocioException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

/**
 * GRASP CONTROLADOR (actor CAJERO): recibe las peticiones de ventanilla, delega en los servicios
 * y decide a que pagina responder. No contiene reglas de negocio.
 * Los depositos y retiros se entregan como COMANDOS al InvocadorCaja (patron Command).
 */
@Controller
@RequestMapping("/cajero")
public class OperacionController {

    private final ClienteService clienteService;
    private final CuentaService cuentaService;
    private final InvocadorCaja invocador;
    private final UsuarioService usuarioService;

    public OperacionController(ClienteService clienteService, CuentaService cuentaService,
                               InvocadorCaja invocador, UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.cuentaService = cuentaService;
        this.invocador = invocador;
        this.usuarioService = usuarioService;
    }

    @GetMapping("")
    public String inicio() {
        return "cajero/inicio";
    }

    // ----------------------------- Clientes -----------------------------

    @GetMapping("/clientes")
    public String clientes(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "cajero/clientes";
    }

    @GetMapping("/clientes/nuevo")
    public String clienteNuevo(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "cajero/cliente-form";
    }

    @PostMapping("/clientes")
    public String clienteGuardar(@ModelAttribute("cliente") Cliente cliente,
                                 @RequestParam String password,
                                 Model model, RedirectAttributes flash) {
        try {
            clienteService.registrarConAcceso(cliente, password);
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            return "cajero/cliente-form";
        }
        flash.addFlashAttribute("ok", "Cliente registrado. Su usuario de acceso es su DNI (" + cliente.getDni() + ")");
        return "redirect:/cajero/clientes";
    }

    // ----------------------------- Cuentas -----------------------------

    @GetMapping("/cuentas")
    public String cuentas(Model model) {
        model.addAttribute("cuentas", cuentaService.listar());
        return "cajero/cuentas";
    }

    @GetMapping("/cuentas/nueva")
    public String cuentaNueva(Model model) {
        cargarFormCuenta(model);
        return "cajero/cuenta-form";
    }

    @PostMapping("/cuentas")
    public String cuentaGuardar(@RequestParam int clienteId,
                                @RequestParam TipoCuenta tipo,
                                @RequestParam(defaultValue = "0") BigDecimal depositoInicial,
                                Model model, RedirectAttributes flash) {
        try {
            Cuenta cuenta = cuentaService.abrirCuenta(clienteId, tipo, depositoInicial);
            flash.addFlashAttribute("ok", "Cuenta " + cuenta.getNumero() + " abierta correctamente");
            return "redirect:/cajero/cuentas/" + cuenta.getId();
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            cargarFormCuenta(model);
            return "cajero/cuenta-form";
        }
    }

    @GetMapping("/cuentas/{id}")
    public String cuentaDetalle(@PathVariable int id, Model model) {
        model.addAttribute("cuenta", cuentaService.buscar(id));
        model.addAttribute("movimientos", cuentaService.historial(id));
        return "cajero/cuenta-detalle";
    }

    // ----------------------------- Operaciones y comprobantes -----------------------------

    @GetMapping("/operaciones")
    public String operacion(Model model) {
        model.addAttribute("cuentas", cuentaService.listar());
        return "cajero/operacion";
    }

    @PostMapping("/operaciones")
    public String operacionEjecutar(@RequestParam int cuentaId,
                                    @RequestParam TipoTransaccion tipo,
                                    @RequestParam BigDecimal monto,
                                    @RequestParam(required = false) String descripcion,
                                    Principal principal, Model model) {
        try {
            // PATRON COMMAND: el controlador arma el comando y el invocador lo ejecuta y lo registra
            ComandoCaja comando = invocador.crearComando(tipo, cuentaId, monto, descripcion);
            Transaccion t = invocador.ejecutar(comando, principal.getName());
            return "redirect:/cajero/comprobante/" + t.getId();
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("cuentas", cuentaService.listar());
            return "cajero/operacion";
        }
    }

    @GetMapping("/comprobante/{id}")
    public String comprobante(@PathVariable int id, Model model) {
        model.addAttribute("t", cuentaService.buscarTransaccion(id));
        return "cajero/comprobante";
    }

    /** El cliente olvido su clave: el cajero verifica su DNI y le entrega una clave temporal. */
    @PostMapping("/clientes/{id}/clave")
    public String restablecerClave(@PathVariable int id, Authentication auth, RedirectAttributes flash) {
        try {
            Usuario usuario = usuarioService.usuarioDeCliente(id);
            String temporal = usuarioService.restablecerClave(usuario.getId(), auth.getName(), RolActual.de(auth));
            flash.addFlashAttribute("claveTemporal", temporal);
            flash.addFlashAttribute("ok", "Clave restablecida para " + usuario.getNombre()
                    + ". Entregale la clave temporal: debera cambiarla al ingresar");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cajero/clientes";
    }

    private void cargarFormCuenta(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("tipos", TipoCuenta.values());
    }
}
