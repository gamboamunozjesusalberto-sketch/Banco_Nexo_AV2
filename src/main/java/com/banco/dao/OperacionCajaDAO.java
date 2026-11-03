package com.banco.dao;

import com.banco.model.OperacionCaja;

import java.util.List;
import java.util.Optional;

public interface OperacionCajaDAO {
    void insertar(OperacionCaja operacion);
    List<OperacionCaja> listar(int limite);
    Optional<OperacionCaja> buscarPorId(int id);
    void marcarExtornada(int id, int extornoId, String extornadoPor, String motivo);
}
