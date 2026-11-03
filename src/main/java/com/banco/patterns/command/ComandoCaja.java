package com.banco.patterns.command;

import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;

import java.math.BigDecimal;

/**
 * PATRON DE COMPORTAMIENTO: COMMAND (interfaz Comando).
 * Cada operacion de ventanilla se convierte en un objeto que tiene todo lo necesario para
 * ejecutarse (cuenta, monto, descripcion) y tambien para deshacerse.
 * El controlador del cajero ya no llama a "depositar" o "retirar": arma el comando y se lo
 * entrega al InvocadorCaja, que lo ejecuta y lo guarda en el historial.
 */
public interface ComandoCaja {

    /** Ejecuta la operacion y devuelve el movimiento registrado (para el comprobante). */
    Transaccion ejecutar();

    /** Deshace la operacion con un movimiento inverso (extorno). En un banco el dinero no se borra. */
    Transaccion deshacer(String motivo);

    TipoTransaccion getTipo();

    int getCuentaId();

    BigDecimal getMonto();
}
