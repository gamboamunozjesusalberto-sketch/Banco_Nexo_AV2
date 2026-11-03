package com.banco.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Transaccion {
    private int id;
    private int cuentaId;
    private TipoTransaccion tipo;
    private BigDecimal monto;
    private BigDecimal saldoResultante;
    private LocalDateTime fecha;
    private String descripcion;
    // Datos de apoyo para pantallas de auditoria y comprobantes
    private String cuentaNumero;
    private String clienteNombre;

    /** true si el movimiento aumenta el saldo. */
    public boolean isIngreso() {
        return tipo != null && tipo.esIngreso();
    }

    /** Numero de comprobante, por ejemplo CP-000015. */
    public String getNumeroComprobante() {
        return String.format("CP-%06d", id);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCuentaId() { return cuentaId; }
    public void setCuentaId(int cuentaId) { this.cuentaId = cuentaId; }
    public TipoTransaccion getTipo() { return tipo; }
    public void setTipo(TipoTransaccion tipo) { this.tipo = tipo; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public BigDecimal getSaldoResultante() { return saldoResultante; }
    public void setSaldoResultante(BigDecimal saldoResultante) { this.saldoResultante = saldoResultante; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getCuentaNumero() { return cuentaNumero; }
    public void setCuentaNumero(String cuentaNumero) { this.cuentaNumero = cuentaNumero; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
}
