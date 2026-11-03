package com.banco.patterns.bridge;

import java.util.List;

/**
 * PATRON ESTRUCTURAL: BRIDGE (Implementador).
 * Es el "COMO" se exporta un reporte. Cada formato sabe convertir un titulo, unas columnas y
 * unas filas en un archivo, sin saber que reporte es.
 */
public interface FormatoReporte {

    byte[] exportar(String titulo, List<String> columnas, List<List<String>> filas);

    String extension();

    String tipoContenido();
}
