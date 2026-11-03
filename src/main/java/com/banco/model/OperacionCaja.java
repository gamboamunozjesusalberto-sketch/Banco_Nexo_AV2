package com.banco.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Registro de una orden de caja ejecutada (patron Command): sirve para auditar y para extornar. */
public class OperacionCaja {

    public enum Estado { EJECUTADA, EXTORNADA }

    private int id;
    private TipoTransaccion comando;
    private int cuentaId;
    private String cuentaNumero;
    private String clienteNombre;
    private BigDecimal monto;
    private String cajero;
    private Integer transaccionId;
    private Estado estado = Estado.EJECUTADA;
    private Integer extornoId;
    private String extornadoPor;
    private String motivoExtorno;
    private LocalDateTime fecha;

    public boolean isExtornable() { return estado == Estado.EJECUTADA; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public TipoTransaccion getComando() { return comando; }
    public void setComando(TipoTransaccion comando) { this.comando = comando; }
    public int getCuentaId() { return cuentaId; }
    public void setCuentaId(int cuentaId) { this.cuentaId = cuentaId; }
    public String getCuentaNumero() { return cuentaNumero; }
    public void setCuentaNumero(String cuentaNumero) { this.cuentaNumero = cuentaNumero; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public String getCajero() { return cajero; }
    public void setCajero(String cajero) { this.cajero = cajero; }
    public Integer getTransaccionId() { return transaccionId; }
    public void setTransaccionId(Integer transaccionId) { this.transaccionId = transaccionId; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public Integer getExtornoId() { return extornoId; }
    public void setExtornoId(Integer extornoId) { this.extornoId = extornoId; }
    public String getExtornadoPor() { return extornadoPor; }
    public void setExtornadoPor(String extornadoPor) { this.extornadoPor = extornadoPor; }
    public String getMotivoExtorno() { return motivoExtorno; }
    public void setMotivoExtorno(String motivoExtorno) { this.motivoExtorno = motivoExtorno; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
