package com.banco.patterns.proxy;

import com.banco.dao.AlertaRiesgoDAO;
import com.banco.dao.BitacoraDAO;
import com.banco.dao.TransaccionDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Rol;
import com.banco.patterns.bridge.FormatoCSV;
import com.banco.patterns.bridge.FormatoPDF;
import com.banco.patterns.bridge.FormatoReporte;
import com.banco.patterns.bridge.Reporte;
import com.banco.patterns.bridge.ReporteBitacora;
import com.banco.patterns.bridge.ReporteGlobal;
import com.banco.patterns.bridge.ReporteRiesgo;
import com.banco.patterns.bridge.ReporteTransacciones;
import com.banco.util.NegocioException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * PATRON PROXY (servicio real): consulta los datos y arma el reporte con el patron BRIDGE.
 * No revisa permisos ni registra nada: de eso se encarga el proxy que va delante.
 */
@Component("servicioReportesReal")
public class ServicioReportesReal implements ServicioReportes {

    private static final int REGISTROS = 1000;

    private final TransaccionDAO transaccionDAO = DAOFactory.getTransaccionDAO();
    private final AlertaRiesgoDAO alertaDAO = DAOFactory.getAlertaRiesgoDAO();
    private final BitacoraDAO bitacoraDAO = DAOFactory.getBitacoraDAO();

    @Override
    public ArchivoReporte generar(TipoReporte tipo, String formato, String usuario, Rol rol) {
        FormatoReporte implementador = switch (formato == null ? "" : formato.toLowerCase()) {
            case "csv" -> new FormatoCSV();
            case "pdf" -> new FormatoPDF();
            default -> throw new NegocioException("Formato no disponible: usa CSV o PDF");
        };
        // BRIDGE: el mismo reporte puede salir en cualquier formato
        Reporte reporte = switch (tipo) {
            case GLOBAL -> new ReporteGlobal(implementador, transaccionDAO.resumenPorTipo());
            case RIESGO -> new ReporteRiesgo(implementador, alertaDAO.listar(false, REGISTROS));
            case TRANSACCIONES -> new ReporteTransacciones(implementador, transaccionDAO.listarTodas(null));
            case BITACORA -> new ReporteBitacora(implementador, bitacoraDAO.listarRecientes(REGISTROS));
        };
        String nombre = reporte.nombreArchivo() + "-" + LocalDate.now() + "." + implementador.extension();
        return new ArchivoReporte(nombre, implementador.tipoContenido(), reporte.generar());
    }
}
