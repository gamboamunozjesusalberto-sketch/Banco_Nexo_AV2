package com.banco.patterns.bridge;

import com.banco.model.AlertaRiesgo;

import java.util.List;

/** PATRON BRIDGE (Abstraccion refinada): alertas de riesgo para el auditor. */
public class ReporteRiesgo extends Reporte {

    private final List<AlertaRiesgo> alertas;

    public ReporteRiesgo(FormatoReporte formato, List<AlertaRiesgo> alertas) {
        super(formato);
        this.alertas = alertas;
    }

    @Override
    public String titulo() { return "Reporte de riesgo (alertas)"; }

    @Override
    public String nombreArchivo() { return "reporte-riesgo"; }

    @Override
    protected List<String> columnas() {
        return List.of("Fecha", "Tipo", "Nivel", "Referencia", "Detalle", "Revisada");
    }

    @Override
    protected List<List<String>> filas() {
        return alertas.stream().map(a -> List.of(fecha(a.getFecha()), a.getTipo().name(), a.getNivel().name(),
                a.getReferencia() == null ? "" : a.getReferencia(), a.getDetalle(), a.isRevisada() ? "Si" : "No")).toList();
    }
}
