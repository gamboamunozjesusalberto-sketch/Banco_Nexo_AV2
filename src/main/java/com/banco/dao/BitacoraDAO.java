package com.banco.dao;

import com.banco.model.RegistroAcceso;

import java.util.List;

public interface BitacoraDAO {
    void registrar(String username, String evento, String detalle);
    List<RegistroAcceso> listarRecientes(int limite);

    /**
     * Intentos fallidos (LOGIN_FALLO) del usuario dentro de los ultimos minutos,
     * contados despues de su ultimo ingreso correcto o desbloqueo.
     */
    int contarFallosRecientes(String username, int minutos);
}
