package com.banco.patterns.proxy;

import com.banco.model.Rol;
import com.banco.service.AuditoriaService;
import com.banco.util.NegocioException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * PATRON ESTRUCTURAL: PROXY (proxy de proteccion).
 * Se pone delante del servicio real con la MISMA interfaz. Antes de dejar pasar la solicitud:
 *  1. Revisa que el rol del usuario pueda sacar ese reporte (segunda barrera, ademas de Spring Security).
 *  2. Deja constancia en la bitacora de quien exporto que y en que formato.
 * Si no tiene permiso, el servicio real ni se entera. Los controladores reciben este proxy (@Primary)
 * creyendo que es el servicio normal.
 */
@Primary
@Component
public class ProxyReportes implements ServicioReportes {

    private final ServicioReportes real;
    private final AuditoriaService auditoria;

    public ProxyReportes(@Qualifier("servicioReportesReal") ServicioReportes real, AuditoriaService auditoria) {
        this.real = real;
        this.auditoria = auditoria;
    }

    @Override
    public ArchivoReporte generar(TipoReporte tipo, String formato, String usuario, Rol rol) {
        if (tipo == null || rol != tipo.getRolPermitido()) {
            auditoria.registrarAcceso(usuario, "EXPORT_DENEGADO", "Intento de exportar " + tipo + " sin permiso");
            throw new NegocioException("No tienes permiso para generar este reporte");
        }
        ArchivoReporte archivo = real.generar(tipo, formato, usuario, rol);
        auditoria.registrarAcceso(usuario, "EXPORTACION", "Reporte " + tipo + " en " + formato.toUpperCase());
        return archivo;
    }
}
