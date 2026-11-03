package com.banco.dao;

import com.banco.model.Notificacion;

import java.util.List;

public interface NotificacionDAO {
    void insertar(Notificacion notificacion);
    List<Notificacion> listarPorCliente(int clienteId, int limite);
    int contarNoLeidas(int clienteId);
    void marcarLeidas(int clienteId);
}
