package com.banco.service;

import com.banco.dao.NotificacionDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Notificacion;
import org.springframework.stereotype.Service;

import java.util.List;

/** Lectura de las notificaciones que deja el observador NotificadorCliente. */
@Service
public class NotificacionService {

    private final NotificacionDAO notificacionDAO = DAOFactory.getNotificacionDAO();

    public List<Notificacion> recientes(int clienteId, int limite) {
        return notificacionDAO.listarPorCliente(clienteId, limite);
    }

    public int noLeidas(int clienteId) {
        return notificacionDAO.contarNoLeidas(clienteId);
    }

    public void marcarLeidas(int clienteId) {
        notificacionDAO.marcarLeidas(clienteId);
    }
}
