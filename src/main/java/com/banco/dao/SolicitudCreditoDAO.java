package com.banco.dao;

import com.banco.model.SolicitudCredito;

import java.util.List;
import java.util.Optional;

public interface SolicitudCreditoDAO {
    void insertar(SolicitudCredito solicitud);
    List<SolicitudCredito> listarTodas();
    List<SolicitudCredito> listarPorCliente(int clienteId);
    Optional<SolicitudCredito> buscarPorId(int id);
    void actualizarResolucion(SolicitudCredito solicitud);
}
