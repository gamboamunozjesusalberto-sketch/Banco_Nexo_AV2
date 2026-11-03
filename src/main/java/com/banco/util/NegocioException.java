package com.banco.util;

/** Error de regla de negocio (dato invalido, saldo insuficiente, duplicado...). Se muestra al usuario. */
public class NegocioException extends RuntimeException {
    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
