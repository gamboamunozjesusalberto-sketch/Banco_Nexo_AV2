package com.banco.model;

/** Persona que ingresa al sistema (cliente o personal del banco). */
public class Usuario {
    private int id;
    private String username;
    private String passwordHash;
    private String nombre;
    private Rol rol;
    private boolean activo = true;
    private boolean debeCambiarClave;
    private boolean bloqueado; // calculado: intentos fallidos recientes (no se guarda)
    private Integer clienteId; // solo para usuarios con rol CLIENTE

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isDebeCambiarClave() { return debeCambiarClave; }
    public void setDebeCambiarClave(boolean debeCambiarClave) { this.debeCambiarClave = debeCambiarClave; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }
}
