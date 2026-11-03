package com.banco.model;

import java.time.LocalDateTime;

/** Una linea de la bitacora de accesos (ingreso, salida o intento fallido). */
public class RegistroAcceso {
    private int id;
    private String username;
    private String evento;
    private String detalle;
    private LocalDateTime fecha;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEvento() { return evento; }
    public void setEvento(String evento) { this.evento = evento; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
