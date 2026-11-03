package com.banco.controller;

import com.banco.model.Cuenta;
import com.banco.model.SolicitudCredito;
import com.banco.model.Transaccion;
import com.banco.service.CreditoService;
import com.banco.service.CuentaService;
import com.banco.service.NotificacionService;
import com.banco.service.UsuarioService;
import com.banco.util.DatabaseException;
import com.banco.util.NegocioException;
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
 * GRASP CONTROLADOR (actor CLIENTE): el cliente consulta sus cuentas y movimientos, transfiere,
 * paga servicios, solicita creditos (y ve su cronograma), revisa sus notificaciones y fija su limite.
 * Siempre trabaja con SU id de cliente (sacado del usuario autenticado), nunca con uno enviado por el navegador.
 */
@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private static final List<String> SERVICIOS = List.of("Luz", "Agua", "Internet", "Telefonia movil", "Gas");

    private final CuentaService cuentaService;
    private final CreditoService creditoService;
    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    public ClienteController(CuentaService cuentaService, CreditoService creditoService,
                             UsuarioService usuarioService, NotificacionService notificacionService) {
        this.cuentaService = cuentaService;
        this.creditoService = creditoService;
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
    }

    // ----------------------------- Consulta -----------------------------

    @GetMapping("")
    public String inicio(Principal principal, Model model) {
        int clienteId = usuarioService.clienteIdDe(principal.getName());
        model.addAttribute("cuentas", cuentaService.listarPorCliente(clienteId));
        model.addAttribute("avisos", notificacionService.recientes(clienteId, 3));
        return "cliente/inicio";
    }

    @PostMapping("/cuentas/{id}/limite")
    public String cambiarLimite(@PathVariable int id, @RequestParam BigDecimal limite,
                                Principal principal, RedirectAttributes flash) {
        try {
            cuentaService.cambiarLimite(usuarioService.clienteIdDe(principal.getName()), id, limite);
            flash.addFlashAttribute("ok", "Limite por operacion actualizado");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cliente/cuentas/" + id;
    }

    // ----------------------------- Notificaciones (patron Observer) -----------------------------

    @GetMapping("/notificaciones")
    public String notificaciones(Principal principal, Model model) {
        int clienteId = usuarioService.clienteIdDe(principal.getName());
        model.addAttribute("lista", notificacionService.recientes(clienteId, 50));
        notificacionService.marcarLeidas(clienteId);
        return "cliente/notificaciones";
    }

    @GetMapping("/cuentas/{id}")
    public String cuentaDetalle(@PathVariable int id, Principal principal, Model model) {
        int clienteId = usuarioService.clienteIdDe(principal.getName());
        model.addAttribute("cuenta", cuentaService.buscarDeCliente(id, clienteId));
        model.addAttribute("movimientos", cuentaService.historial(id));
        return "cliente/cuenta-detalle";
    }

    // ----------------------------- Transferencias -----------------------------

    @GetMapping("/transferir")
    public String transferir(Principal principal, Model model) {
        cargarCuentas(principal, model);
        return "cliente/transferir";
    }

    @PostMapping("/transferir")
    public String transferirEjecutar(@RequestParam int cuentaOrigenId,
                                     @RequestParam String cuentaDestino,
                                     @RequestParam BigDecimal monto,
                                     @RequestParam(required = false) String descripcion,
                                     @RequestParam(required = false) String claveConfirmacion,
                                     Principal principal, Model model, RedirectAttributes flash) {
        try {
            Transaccion t = cuentaService.transferir(usuarioService.clienteIdDe(principal.getName()),
                    cuentaOrigenId, cuentaDestino, monto, descripcion, principal.getName(), claveConfirmacion);
            flash.addFlashAttribute("ok", "Transferencia realizada. Comprobante " + t.getNumeroComprobante());
            return "redirect:/cliente/cuentas/" + cuentaOrigenId;
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            cargarCuentas(principal, model);
            return "cliente/transferir";
        }
    }

    // ----------------------------- Pago de servicios -----------------------------

    @GetMapping("/pagos")
    public String pagos(Principal principal, Model model) {
        cargarCuentas(principal, model);
        model.addAttribute("servicios", SERVICIOS);
        return "cliente/pagos";
    }

    @PostMapping("/pagos")
    public String pagar(@RequestParam int cuentaId,
                        @RequestParam String servicio,
                        @RequestParam String referencia,
                        @RequestParam BigDecimal monto,
                        @RequestParam(required = false) String claveConfirmacion,
                        Principal principal, Model model, RedirectAttributes flash) {
        try {
            Transaccion t = cuentaService.pagarServicio(usuarioService.clienteIdDe(principal.getName()),
                    cuentaId, servicio, referencia, monto, principal.getName(), claveConfirmacion);
            flash.addFlashAttribute("ok", "Pago realizado. Comprobante " + t.getNumeroComprobante());
            return "redirect:/cliente/cuentas/" + cuentaId;
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            cargarCuentas(principal, model);
            model.addAttribute("servicios", SERVICIOS);
            return "cliente/pagos";
        }
    }

    // ----------------------------- Creditos -----------------------------

    @GetMapping("/creditos")
    public String creditos(Principal principal, Model model) {
        cargarCreditos(usuarioService.clienteIdDe(principal.getName()), model);
        return "cliente/creditos";
    }

    @PostMapping("/creditos")
    public String solicitarCredito(@RequestParam BigDecimal monto,
                                   @RequestParam int plazoMeses,
                                   @RequestParam String motivo,
                                   @RequestParam(required = false) Integer cuentaDestinoId,
                                   Principal principal, Model model, RedirectAttributes flash) {
        int clienteId = usuarioService.clienteIdDe(principal.getName());
        try {
            creditoService.solicitar(clienteId, monto, plazoMeses, motivo, cuentaDestinoId);
            flash.addFlashAttribute("ok", "Solicitud enviada. Un asesor la evaluara y te avisaremos el resultado");
            return "redirect:/cliente/creditos";
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            cargarCreditos(clienteId, model);
            return "cliente/creditos";
        }
    }

    /** Cronograma de pagos (GRASP Experto: la solicitud calcula sus propias cuotas). */
    @GetMapping("/creditos/{id}")
    public String cronograma(@PathVariable int id, Principal principal, Model model) {
        SolicitudCredito s = creditoService.buscar(id);
        if (s.getClienteId() != usuarioService.clienteIdDe(principal.getName())) {
            throw new NegocioException("La solicitud no pertenece al cliente");
        }
        model.addAttribute("s", s);
        return "cliente/cronograma";
    }

    private void cargarCreditos(int clienteId, Model model) {
        model.addAttribute("solicitudes", creditoService.listarPorCliente(clienteId));
        model.addAttribute("cuentas", cuentaService.listarPorCliente(clienteId));
        model.addAttribute("tasa", creditoService.tasaVigente());
    }

    private void cargarCuentas(Principal principal, Model model) {
        List<Cuenta> cuentas = cuentaService.listarPorCliente(usuarioService.clienteIdDe(principal.getName()));
        model.addAttribute("cuentas", cuentas);
        model.addAttribute("umbral", cuentaService.umbralConfirmacion());
    }
}
