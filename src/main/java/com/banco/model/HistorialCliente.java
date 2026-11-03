package com.banco.model;

import java.util.List;

/** Datos del cliente que el asesor revisa antes de evaluar un credito: cuentas, movimientos y solicitudes. */
public class HistorialCliente {
    private final Cliente cliente;
    private final List<Cuenta> cuentas;
    private final List<Transaccion> movimientos;
    private final List<SolicitudCredito> solicitudes;

    public HistorialCliente(Cliente cliente, List<Cuenta> cuentas, List<Transaccion> movimientos,
                            List<SolicitudCredito> solicitudes) {
        this.cliente = cliente;
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.solicitudes = solicitudes;
    }

    public Cliente getCliente() { return cliente; }
    public List<Cuenta> getCuentas() { return cuentas; }
    public List<Transaccion> getMovimientos() { return movimientos; }
    public List<SolicitudCredito> getSolicitudes() { return solicitudes; }
}
