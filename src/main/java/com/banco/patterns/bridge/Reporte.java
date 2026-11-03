package com.banco.patterns.bridge;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PATRON ESTRUCTURAL: BRIDGE (Abstraccion).
 * Es el "QUE" se reporta. Tiene una referencia al formato (el "puente") y le delega la exportacion.
 * Sin Bridge habria una clase por cada combinacion (ReporteGlobalCSV, ReporteGlobalPDF,
 * ReporteRiesgoCSV...). Con Bridge, 4 reportes x 2 formatos = 6 clases en lugar de 8,
 * y agregar Excel es UNA clase nueva, no cuatro. (Idea tomada de Revolut: estados de cuenta en CSV y PDF.)
 */
public abstract class Reporte {

    protected static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    protected final FormatoReporte formato;

    protected Reporte(FormatoReporte formato) {
        this.formato = formato;
    }

    public abstract String titulo();

    public abstract String nombreArchivo();

    protected abstract List<String> columnas();

    protected abstract List<List<String>> filas();

    /** Metodo plantilla: arma los datos y se los entrega al formato elegido. */
    public final byte[] generar() {
        return formato.exportar(titulo(), columnas(), filas());
    }

    public FormatoReporte getFormato() {
        return formato;
    }

    protected static String soles(BigDecimal monto) {
        return monto == null ? "" : "S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    protected static String fecha(LocalDateTime fecha) {
        return fecha == null ? "" : fecha.format(FECHA);
    }
}
