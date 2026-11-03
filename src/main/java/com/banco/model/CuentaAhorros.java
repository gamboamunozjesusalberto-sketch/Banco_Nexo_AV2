package com.banco.model;

import java.math.BigDecimal;

/** Cuenta de ahorros: no permite quedar en negativo. */
public class CuentaAhorros extends Cuenta {
    @Override
    public TipoCuenta getTipo() { return TipoCuenta.AHORROS; }

    @Override
    public BigDecimal getSobregiroPermitido() { return BigDecimal.ZERO; }
}
