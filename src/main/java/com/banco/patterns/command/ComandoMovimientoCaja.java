package com.banco.patterns.command;

import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;
import com.banco.service.CuentaService;

import java.math.BigDecimal;

/**
 * PATRON COMMAND: base comun de los comandos de ventanilla.
 * Guarda lo que todo comando necesita (receptor, cuenta, monto, descripcion) y define ejecutar/deshacer
 * una sola vez. Cada comando concreto solo dice QUE movimiento hace y cual es su movimiento inverso,
 * asi no se repite el mismo codigo en ComandoDeposito y ComandoRetiro.
 */
public abstract class ComandoMovimientoCaja implements ComandoCaja {

    private final CuentaService receptor;
    private final int cuentaId;
    private final BigDecimal monto;
    private final String descripcion;

    protected ComandoMovimientoCaja(CuentaService receptor, int cuentaId, BigDecimal monto, String descripcion) {
        this.receptor = receptor;
        this.cuentaId = cuentaId;
        this.monto = monto;
        this.descripcion = descripcion;
    }

    /** Movimiento inverso que anula a este (el extorno). */
    protected abstract TipoTransaccion tipoInverso();

    @Override
    public Transaccion ejecutar() {
        return receptor.moverEnCaja(cuentaId, getTipo(), monto, descripcion);
    }

    /** En un banco el dinero no se borra: deshacer = registrar el movimiento inverso por el mismo monto. */
    @Override
    public Transaccion deshacer(String motivo) {
        return receptor.moverEnCaja(cuentaId, tipoInverso(), monto, tipoInverso().getNombre() + ": " + motivo);
    }

    @Override
    public int getCuentaId() { return cuentaId; }

    @Override
    public BigDecimal getMonto() { return monto; }
}
