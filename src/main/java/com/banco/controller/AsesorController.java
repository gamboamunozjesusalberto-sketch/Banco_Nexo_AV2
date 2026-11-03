package com.banco.controller;

import com.banco.model.SolicitudCredito;
import com.banco.service.ClienteService;
import com.banco.service.CreditoService;
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

import java.security.Principal;

/**
 * GRASP CONTROLADOR (actor ASESOR DE CREDITO): evalua solicitudes (aprobar o rechazar con
 * observaciones) y consulta el historial del cliente.
 */
@Controller
@RequestMapping("/asesor")
public class AsesorController {

    private final CreditoService creditoService;
    private final ClienteService clienteService;
    public AsesorController(CreditoService creditoService, ClienteService clienteService) {
        this.creditoService = creditoService;
        this.clienteService = clienteService;
    }

    @GetMapping("")
    public String solicitudes(Model model) {
        model.addAttribute("solicitudes", creditoService.listarTodas());
        return "asesor/solicitudes";
    }

    @GetMapping("/solicitudes/{id}")
    public String solicitud(@PathVariable int id, Model model) {
        SolicitudCredito s = creditoService.buscar(id);
        model.addAttribute("s", s);
        model.addAttribute("historial", creditoService.historialDeCliente(s.getClienteId()));
        return "asesor/solicitud";
    }

    /** PATRON STATE: PENDIENTE -> EN_EVALUACION. */
    @PostMapping("/solicitudes/{id}/evaluar")
    public String evaluar(@PathVariable int id, Principal principal, RedirectAttributes flash) {
        try {
            creditoService.iniciarEvaluacion(id, principal.getName());
            flash.addFlashAttribute("ok", "Solicitud en evaluacion. Revisa el historial del cliente y decide");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/asesor/solicitudes/" + id;
    }

    @PostMapping("/solicitudes/{id}/resolver")
    public String resolver(@PathVariable int id,
                           @RequestParam String decision,
                           @RequestParam(required = false) String observaciones,
                           Principal principal, RedirectAttributes flash) {
        try {
            creditoService.resolver(id, "APROBAR".equals(decision), observaciones, principal.getName());
            flash.addFlashAttribute("ok", "APROBAR".equals(decision)
                    ? "Solicitud aprobada: el dinero ya se abono en la cuenta del cliente"
                    : "Solicitud rechazada. El cliente recibira la notificacion");
            return "redirect:/asesor";
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
            return "redirect:/asesor/solicitudes/" + id;
        }
    }

    @GetMapping("/clientes")
    public String clientes(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "asesor/clientes";
    }

    @GetMapping("/clientes/{id}")
    public String historial(@PathVariable int id, Model model) {
        model.addAttribute("historial", creditoService.historialDeCliente(id));
        return "asesor/historial";
    }
}
