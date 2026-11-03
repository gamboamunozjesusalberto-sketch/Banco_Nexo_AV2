package com.banco.patterns.command;

import com.banco.model.TipoTransaccion;
import com.banco.service.CuentaService;

import java.math.BigDecimal;

/** PATRON COMMAND (comando concreto): retiro en ventanilla. El receptor es CuentaService. */
public class ComandoRetiro extends ComandoMovimientoCaja {

    public ComandoRetiro(CuentaService receptor, int cuentaId, BigDecimal monto, String descripcion) {
        super(receptor, cuentaId, monto, descripcion);
    }

    @Override
    public TipoTransaccion getTipo() { return TipoTransaccion.RETIRO; }

    @Override
    protected TipoTransaccion tipoInverso() { return TipoTransaccion.EXTORNO_RETIRO; }
}
