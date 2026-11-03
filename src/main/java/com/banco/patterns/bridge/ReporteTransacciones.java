package com.banco.patterns.bridge;

import com.banco.model.Transaccion;

import java.util.List;

/** PATRON BRIDGE (Abstraccion refinada): movimientos registrados, para el auditor. */
public class ReporteTransacciones extends Reporte {

    private final List<Transaccion> transacciones;

    public ReporteTransacciones(FormatoReporte formato, List<Transaccion> transacciones) {
        super(formato);
        this.transacciones = transacciones;
    }

    @Override
    public String titulo() { return "Reporte de transacciones"; }

    @Override
    public String nombreArchivo() { return "reporte-transacciones"; }

    @Override
    protected List<String> columnas() {
        return List.of("Comprobante", "Fecha", "Cuenta", "Cliente", "Tipo", "Monto", "Saldo", "Descripcion");
    }

    @Override
    protected List<List<String>> filas() {
        return transacciones.stream().map(t -> List.of(t.getNumeroComprobante(), fecha(t.getFecha()),
                t.getCuentaNumero(), t.getClienteNombre(), t.getTipo().name(), soles(t.getMonto()),
                soles(t.getSaldoResultante()), t.getDescripcion() == null ? "" : t.getDescripcion())).toList();
    }
}
