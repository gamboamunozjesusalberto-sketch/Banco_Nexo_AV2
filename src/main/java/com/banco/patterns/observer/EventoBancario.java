package com.banco.patterns.observer;

import java.math.BigDecimal;

/**
 * PATRON OBSERVER: el "mensaje" que el publicador envia a todos sus observadores.
 * Describe algo que paso en el banco; cada observador decide si le interesa.
 */
public class EventoBancario {

    public enum Tipo {
        /** Cualquier movimiento de dinero en una cuenta (deposito, retiro, transferencia, pago...). */
        MOVIMIENTO_REGISTRADO,
        TRANSFERENCIA_RECIBIDA,
        CREDITO_DESEMBOLSADO,
        CREDITO_RECHAZADO,
        LOGIN_FALLIDO
    }

    private final Tipo tipo;
    private Integer clienteId;
    private Integer cuentaId;
    private String cuentaNumero;
    private BigDecimal monto;
    private String username;
    private String detalle;

    public EventoBancario(Tipo tipo) {
        this.tipo = tipo;
    }

    // Metodos encadenables para armar el evento en una sola linea
    public EventoBancario cliente(Integer clienteId) { this.clienteId = clienteId; return this; }
    public EventoBancario cuenta(Integer cuentaId, String numero) { this.cuentaId = cuentaId; this.cuentaNumero = numero; return this; }
    public EventoBancario monto(BigDecimal monto) { this.monto = monto; return this; }
    public EventoBancario usuario(String username) { this.username = username; return this; }
    public EventoBancario detalle(String detalle) { this.detalle = detalle; return this; }

    public Tipo getTipo() { return tipo; }
    public Integer getClienteId() { return clienteId; }
    public Integer getCuentaId() { return cuentaId; }
    public String getCuentaNumero() { return cuentaNumero; }
    public BigDecimal getMonto() { return monto; }
    public String getUsername() { return username; }
    public String getDetalle() { return detalle; }
}
