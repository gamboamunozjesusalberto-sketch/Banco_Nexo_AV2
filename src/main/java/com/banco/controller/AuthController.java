package com.banco.controller;

import com.banco.model.Cliente;
import com.banco.service.ClienteService;
import com.banco.service.UsuarioService;
import com.banco.util.DatabaseException;
import com.banco.util.NegocioException;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Entrada al sistema: login, registro en linea, cambio de clave y redireccion segun el rol. */
@Controller
public class AuthController {

    /** Atributo de sesion que obliga a cambiar la clave temporal antes de usar el sistema. */
    public static final String DEBE_CAMBIAR_CLAVE = "DEBE_CAMBIAR_CLAVE";

    private final ClienteService clienteService;
    private final UsuarioService usuarioService;

    public AuthController(ClienteService clienteService, UsuarioService usuarioService) {
        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
    }

    /** Despues de ingresar, cada actor va a su propio panel. */
    @GetMapping("/")
    public String inicio(Authentication authentication) {
        for (GrantedAuthority autoridad : authentication.getAuthorities()) {
            switch (autoridad.getAuthority()) {
                case "ROLE_ADMINISTRADOR": return "redirect:/admin";
                case "ROLE_CLIENTE": return "redirect:/cliente";
                case "ROLE_ASESOR": return "redirect:/asesor";
                case "ROLE_CAJERO": return "redirect:/cajero";
                case "ROLE_AUDITOR": return "redirect:/auditor";
                default: break;
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }

    // ----------------------------- Registro en linea -----------------------------

    @GetMapping("/registro")
    public String registro(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute("cliente") Cliente cliente,
                            @RequestParam String password, @RequestParam String confirmacion, Model model) {
        try {
            clienteService.registrarseEnLinea(cliente, password, confirmacion);
            return "redirect:/login?registrado";
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            return "registro";
        }
    }

    // ----------------------------- Cambiar mi clave -----------------------------

    @GetMapping("/perfil/clave")
    public String cambiarClave(HttpSession sesion, Model model) {
        model.addAttribute("temporal", Boolean.TRUE.equals(sesion.getAttribute(DEBE_CAMBIAR_CLAVE)));
        return "perfil-clave";
    }

    @PostMapping("/perfil/clave")
    public String cambiarClave(@RequestParam String actual, @RequestParam String nueva,
                               @RequestParam String confirmacion, Authentication auth, HttpSession sesion,
                               Model model, RedirectAttributes flash) {
        try {
            usuarioService.cambiarClave(auth.getName(), actual, nueva, confirmacion);
            sesion.removeAttribute(DEBE_CAMBIAR_CLAVE);
            flash.addFlashAttribute("ok", "Tu clave se cambio correctamente");
            return "redirect:/";
        } catch (NegocioException | DatabaseException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("temporal", Boolean.TRUE.equals(sesion.getAttribute(DEBE_CAMBIAR_CLAVE)));
            return "perfil-clave";
        }
    }
}
