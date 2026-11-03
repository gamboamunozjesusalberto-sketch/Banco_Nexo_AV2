package com.banco.model;

/** Valor de configuracion del sistema guardado en la base de datos (en lugar de numeros magicos). */
public class Parametro {
    private String clave;
    private String valor;
    private String descripcion;

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
