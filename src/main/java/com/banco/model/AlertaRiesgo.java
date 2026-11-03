package com.banco.model;

import java.time.LocalDateTime;

/** Alerta para el auditor (la genera el observador MonitorRiesgo). */
public class AlertaRiesgo {

    public enum Tipo { MONTO_ALTO, INTENTOS_FALLIDOS, OPERACIONES_SEGUIDAS }

    public enum Nivel { MEDIO, ALTO }

    private int id;
    private Tipo tipo;
    private Nivel nivel;
    private String detalle;
    private String referencia;
    private boolean revisada;
    private LocalDateTime fecha;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public Tipo getTipo() { return tipo; }
    public void setTipo(Tipo tipo) { this.tipo = tipo; }
    public Nivel getNivel() { return nivel; }
    public void setNivel(Nivel nivel) { this.nivel = nivel; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public boolean isRevisada() { return revisada; }
    public void setRevisada(boolean revisada) { this.revisada = revisada; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
