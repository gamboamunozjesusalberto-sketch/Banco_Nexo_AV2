package com.banco.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila del cronograma de pagos de un credito. */
public class CuotaCredito {

    private final int numero;
    private final LocalDate fechaVencimiento;
    private final BigDecimal cuota;
    private final BigDecimal interes;
    private final BigDecimal amortizacion;
    private final BigDecimal saldoPendiente;

    public CuotaCredito(int numero, LocalDate fechaVencimiento, BigDecimal cuota, BigDecimal interes,
                        BigDecimal amortizacion, BigDecimal saldoPendiente) {
        this.numero = numero;
        this.fechaVencimiento = fechaVencimiento;
        this.cuota = cuota;
        this.interes = interes;
        this.amortizacion = amortizacion;
        this.saldoPendiente = saldoPendiente;
    }

    public int getNumero() { return numero; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public BigDecimal getCuota() { return cuota; }
    public BigDecimal getInteres() { return interes; }
    public BigDecimal getAmortizacion() { return amortizacion; }
    public BigDecimal getSaldoPendiente() { return saldoPendiente; }
}
