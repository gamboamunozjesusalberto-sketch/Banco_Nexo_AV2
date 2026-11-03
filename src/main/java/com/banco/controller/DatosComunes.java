package com.banco.controller;

import com.banco.model.Rol;
import com.banco.service.AuditoriaService;
import com.banco.service.NotificacionService;
import com.banco.service.UsuarioService;
import com.banco.util.RolActual;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos que se muestran en el menu de todas las paginas: campana del cliente y alertas del auditor. */
@ControllerAdvice
public class DatosComunes {

    private final NotificacionService notificacionService;
    private final AuditoriaService auditoriaService;
    private final UsuarioService usuarioService;

    public DatosComunes(NotificacionService notificacionService, AuditoriaService auditoriaService,
                        UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.auditoriaService = auditoriaService;
        this.usuarioService = usuarioService;
    }

    @ModelAttribute("contadorMenu")
    public Integer contadorMenu(Authentication auth) {
        try {
            Rol rol = RolActual.de(auth);
            if (rol == Rol.CLIENTE) {
                return notificacionService.noLeidas(usuarioService.clienteIdDe(auth.getName()));
            }
            if (rol == Rol.AUDITOR) {
                return auditoriaService.alertasPendientes();
            }
        } catch (RuntimeException ignorada) {
            // el contador es solo informativo; si falla, la pagina igual se muestra
        }
        return 0;
    }

    /** Nombre del usuario conectado, para saludarlo ("Hola, Ana Torres"). */
    @ModelAttribute("nombreUsuario")
    public String nombreUsuario(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        try {
            return usuarioService.nombreDe(auth.getName());
        } catch (RuntimeException ignorada) {
            return auth.getName();
        }
    }
}
