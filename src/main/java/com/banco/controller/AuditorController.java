package com.banco.controller;

import com.banco.model.TipoTransaccion;
import com.banco.patterns.proxy.ArchivoReporte;
import com.banco.patterns.proxy.ServicioReportes;
import com.banco.patterns.proxy.TipoReporte;
import com.banco.service.AuditoriaService;
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

import java.security.Principal;

/**
 * GRASP CONTROLADOR (actor AUDITOR). Sus cuatro funciones:
 *  1. Revisar transacciones.
 *  2. Consultar la bitacora de accesos.
 *  3. Revisar las alertas de riesgo (patron Observer). Idea tomada de Bank of America.
 *  4. Exportar informacion en CSV o PDF (patrones Bridge y Proxy). Idea tomada de Revolut.
 */
@Controller
@RequestMapping("/auditor")
public class AuditorController {

    private final AuditoriaService auditoriaService;
    private final ServicioReportes reportes;

    public AuditorController(AuditoriaService auditoriaService, ServicioReportes reportes) {
        this.auditoriaService = auditoriaService;
        this.reportes = reportes;
    }

    @GetMapping("")
    public String inicio() {
        return "redirect:/auditor/alertas";
    }

    @GetMapping("/transacciones")
    public String transacciones(@RequestParam(required = false) TipoTransaccion tipo, Model model) {
        model.addAttribute("transacciones", auditoriaService.transacciones(tipo));
        model.addAttribute("tipos", TipoTransaccion.values());
        model.addAttribute("tipoSeleccionado", tipo);
        return "auditor/transacciones";
    }

    @GetMapping("/bitacora")
    public String bitacora(Model model) {
        model.addAttribute("registros", auditoriaService.bitacora());
        return "auditor/bitacora";
    }

    @GetMapping("/alertas")
    public String alertas(@RequestParam(defaultValue = "false") boolean todas, Model model) {
        model.addAttribute("lista", auditoriaService.alertas(!todas));
        model.addAttribute("todas", todas);
        return "auditor/alertas";
    }

    @PostMapping("/alertas/{id}/revisar")
    public String revisar(@PathVariable int id, Principal principal, RedirectAttributes flash) {
        try {
            auditoriaService.marcarAlertaRevisada(id, principal.getName());
            flash.addFlashAttribute("ok", "Alerta marcada como revisada");
        } catch (NegocioException | DatabaseException e) {
            flash.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/auditor/alertas";
    }

    /** El Proxy decide si este rol puede sacar el reporte pedido (prueba: ?tipo=GLOBAL da "sin permiso"). */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@RequestParam TipoReporte tipo,
                                           @RequestParam(defaultValue = "pdf") String formato,
                                           Authentication auth) {
        ArchivoReporte archivo = reportes.generar(tipo, formato, auth.getName(), RolActual.de(auth));
        return Descargas.archivo(archivo);
    }
}
