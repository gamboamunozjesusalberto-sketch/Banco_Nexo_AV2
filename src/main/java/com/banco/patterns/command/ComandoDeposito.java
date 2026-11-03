package com.banco.patterns.command;

import com.banco.model.TipoTransaccion;
import com.banco.service.CuentaService;

import java.math.BigDecimal;

/** PATRON COMMAND (comando concreto): deposito en ventanilla. El receptor es CuentaService. */
public class ComandoDeposito extends ComandoMovimientoCaja {

    public ComandoDeposito(CuentaService receptor, int cuentaId, BigDecimal monto, String descripcion) {
        super(receptor, cuentaId, monto, descripcion);
    }

    @Override
    public TipoTransaccion getTipo() { return TipoTransaccion.DEPOSITO; }

    @Override
    protected TipoTransaccion tipoInverso() { return TipoTransaccion.EXTORNO_DEPOSITO; }
}
