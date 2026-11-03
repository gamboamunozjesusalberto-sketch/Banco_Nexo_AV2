package com.banco.dao;

import com.banco.model.AlertaRiesgo;

import java.util.List;

public interface AlertaRiesgoDAO {
    void insertar(AlertaRiesgo alerta);
    /** Si soloPendientes es true, solo las que el auditor aun no reviso. */
    List<AlertaRiesgo> listar(boolean soloPendientes, int limite);
    int contarPendientes();
    void marcarRevisada(int id);
}
