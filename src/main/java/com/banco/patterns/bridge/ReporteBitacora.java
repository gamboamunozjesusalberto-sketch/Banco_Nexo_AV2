package com.banco.patterns.bridge;

import com.banco.model.RegistroAcceso;

import java.util.List;

/** PATRON BRIDGE (Abstraccion refinada): bitacora de accesos, para el auditor. */
public class ReporteBitacora extends Reporte {

    private final List<RegistroAcceso> registros;

    public ReporteBitacora(FormatoReporte formato, List<RegistroAcceso> registros) {
        super(formato);
        this.registros = registros;
    }

    @Override
    public String titulo() { return "Bitacora de accesos"; }

    @Override
    public String nombreArchivo() { return "bitacora-accesos"; }

    @Override
    protected List<String> columnas() {
        return List.of("Fecha", "Usuario", "Evento", "Detalle");
    }

    @Override
    protected List<List<String>> filas() {
        return registros.stream().map(r -> List.of(fecha(r.getFecha()), r.getUsername(), r.getEvento(),
                r.getDetalle() == null ? "" : r.getDetalle())).toList();
    }
}
