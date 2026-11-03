package com.banco.patterns.bridge;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** PATRON BRIDGE (Abstraccion refinada): resumen global de operaciones para el administrador. */
public class ReporteGlobal extends Reporte {

    private final List<Object[]> resumenPorTipo;

    public ReporteGlobal(FormatoReporte formato, List<Object[]> resumenPorTipo) {
        super(formato);
        this.resumenPorTipo = resumenPorTipo;
    }

    @Override
    public String titulo() { return "Reporte global de operaciones"; }

    @Override
    public String nombreArchivo() { return "reporte-global"; }

    @Override
    protected List<String> columnas() {
        return List.of("Tipo de operacion", "Cantidad", "Monto total");
    }

    @Override
    protected List<List<String>> filas() {
        List<List<String>> filas = new ArrayList<>();
        int cantidad = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] r : resumenPorTipo) {
            String tipo = String.valueOf(r[0]);
            filas.add(List.of(com.banco.model.TipoTransaccion.valueOf(tipo).getNombre(), String.valueOf(r[1]),
                    soles((BigDecimal) r[2])));
            if ("TRANSFERENCIA_RECIBIDA".equals(tipo)) {
                continue; // la transferencia ya se conto en TRANSFERENCIA_ENVIADA
            }
            cantidad += (Integer) r[1];
            total = total.add((BigDecimal) r[2]);
        }
        filas.add(List.of("TOTAL (transferencias contadas una vez)", String.valueOf(cantidad), soles(total)));
        return filas;
    }
}
