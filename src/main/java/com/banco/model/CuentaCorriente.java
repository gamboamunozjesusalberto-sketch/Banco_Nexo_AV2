package com.banco.model;

import java.math.BigDecimal;

/** Cuenta corriente: permite un sobregiro de hasta S/ 500. */
public class CuentaCorriente extends Cuenta {

    /** Antes era un "numero magico" escrito dentro del metodo; ahora es una constante con nombre. */
    public static final BigDecimal SOBREGIRO_MAXIMO = new BigDecimal("500.00");
    @Override
    public TipoCuenta getTipo() { return TipoCuenta.CORRIENTE; }

    @Override
    public BigDecimal getSobregiroPermitido() { return SOBREGIRO_MAXIMO; }
}
