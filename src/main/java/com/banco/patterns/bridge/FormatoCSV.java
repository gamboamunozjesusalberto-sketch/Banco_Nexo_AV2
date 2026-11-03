package com.banco.patterns.bridge;

import java.nio.charset.StandardCharsets;
import java.util.List;

/** PATRON BRIDGE (Implementador concreto): exporta a CSV, que se abre en Excel. */
public class FormatoCSV implements FormatoReporte {

    /** Marca BOM para que Excel reconozca las tildes (UTF-8). */
    private static final String BOM = "﻿";

    @Override
    public byte[] exportar(String titulo, List<String> columnas, List<List<String>> filas) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append(linea(columnas));
        for (List<String> fila : filas) {
            sb.append(linea(fila));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String linea(List<String> valores) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(';'); // Excel en espanol usa punto y coma como separador
            }
            sb.append(escapar(valores.get(i)));
        }
        return sb.append("\r\n").toString();
    }

    private String escapar(String valor) {
        String v = valor == null ? "" : valor;
        // evita la "inyeccion de formulas" al abrir el CSV en Excel
        if (!v.isEmpty() && "=+-@".indexOf(v.charAt(0)) >= 0 && !v.matches("-?\\d+(\\.\\d+)?")) {
            v = "'" + v;
        }
        if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    @Override
    public String extension() { return "csv"; }

    @Override
    public String tipoContenido() { return "text/csv; charset=UTF-8"; }
}
