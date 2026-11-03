package com.banco.model;

public enum TipoTransaccion {
    DEPOSITO, RETIRO, TRANSFERENCIA_ENVIADA, TRANSFERENCIA_RECIBIDA, PAGO_SERVICIO,
    DESEMBOLSO_CREDITO,
    /** Anula un deposito de caja: saca el dinero que entro por error (patron Command, deshacer). */
    EXTORNO_DEPOSITO,
    /** Anula un retiro de caja: devuelve el dinero que salio por error (patron Command, deshacer). */
    EXTORNO_RETIRO,
    /** Ingreso en la cuenta interna NEXO Recaudacion por cada pago de servicio de un cliente. */
    RECAUDACION_SERVICIO;

    /** true si el movimiento aumenta el saldo de la cuenta. */
    public boolean esIngreso() {
        return this == DEPOSITO || this == TRANSFERENCIA_RECIBIDA || this == DESEMBOLSO_CREDITO || this == EXTORNO_RETIRO
                || this == RECAUDACION_SERVICIO;
    }

    /** Texto amigable para mostrar en pantalla. */
    public String getNombre() {
        return switch (this) {
            case DEPOSITO -> "Deposito";
            case RETIRO -> "Retiro";
            case TRANSFERENCIA_ENVIADA -> "Transferencia enviada";
            case TRANSFERENCIA_RECIBIDA -> "Transferencia recibida";
            case PAGO_SERVICIO -> "Pago de servicio";
            case DESEMBOLSO_CREDITO -> "Desembolso de credito";
            case EXTORNO_DEPOSITO -> "Extorno de deposito";
            case EXTORNO_RETIRO -> "Extorno de retiro";
            case RECAUDACION_SERVICIO -> "Recaudacion de servicio";
        };
    }
}
