package com.banco.model;

import com.banco.util.NegocioException;
import com.banco.util.Validaciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * GRASP EXPERTO: la cuenta tiene el saldo y el limite, por lo tanto ella misma calcula y valida
 * depositos, retiros y el limite por operacion.
 * SOLID (O y L): las subclases CuentaAhorros y CuentaCorriente reemplazan a Cuenta sin romper nada;
 * un nuevo tipo de cuenta se agrega sin modificar este codigo.
 */
public abstract class Cuenta {

    /** Tope maximo que un cliente puede configurar como limite por operacion (evita numeros magicos). */
    public static final BigDecimal LIMITE_MAXIMO_CONFIGURABLE = new BigDecimal("50000");

    private int id;
    private String numero;
    private BigDecimal saldo = BigDecimal.ZERO;
    private BigDecimal limiteOperacion = new BigDecimal("5000");
    private int clienteId;
    private String clienteNombre;
    private LocalDateTime fechaApertura;

    /** PATRON FACTORY (metodo de fabrica): crea la subclase correcta segun el tipo de cuenta. */
    public static Cuenta crear(TipoCuenta tipo) {
        return switch (tipo) {
            case AHORROS -> new CuentaAhorros();
            case CORRIENTE -> new CuentaCorriente();
        };
    }

    public abstract TipoCuenta getTipo();

    /** Monto maximo que la cuenta puede quedar en negativo. */
    public abstract BigDecimal getSobregiroPermitido();

    public void depositar(BigDecimal monto) {
        validarMonto(monto);
        saldo = saldo.add(monto);
    }

    public void retirar(BigDecimal monto) {
        validarMonto(monto);
        BigDecimal nuevoSaldo = saldo.subtract(monto);
        if (nuevoSaldo.compareTo(getSobregiroPermitido().negate()) < 0) {
            throw new NegocioException("Saldo insuficiente. Disponible para retirar: S/ "
                    + getSaldoDisponible().setScale(2, RoundingMode.HALF_UP));
        }
        saldo = nuevoSaldo;
    }

    /**
     * Limite por operacion del canal digital (idea tomada de "Modo Calle" de Interbank):
     * el cliente fija cuanto puede mover como maximo en una sola operacion.
     */
    public void validarLimiteOperacion(BigDecimal monto) {
        if (monto != null && monto.compareTo(limiteOperacion) > 0) {
            throw new NegocioException("El monto supera el limite por operacion de la cuenta (S/ "
                    + limiteOperacion.setScale(2, RoundingMode.HALF_UP)
                    + ")");
        }
    }

    public BigDecimal getSaldoDisponible() {
        return saldo.add(getSobregiroPermitido());
    }

    private void validarMonto(BigDecimal monto) {
        if (!Validaciones.montoValido(monto)) {
            throw new NegocioException("El monto debe ser mayor a 0 y tener maximo 2 decimales");
        }
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public BigDecimal getSaldo() { return saldo; }
    public void setSaldo(BigDecimal saldo) { this.saldo = saldo; }
    public BigDecimal getLimiteOperacion() { return limiteOperacion; }
    public void setLimiteOperacion(BigDecimal limiteOperacion) { this.limiteOperacion = limiteOperacion; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime fechaApertura) { this.fechaApertura = fechaApertura; }
}
