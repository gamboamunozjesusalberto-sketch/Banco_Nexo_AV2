package com.banco.dao;

import com.banco.model.Parametro;

import java.util.List;
import java.util.Optional;

public interface ParametroDAO {
    List<Parametro> listar();
    Optional<String> obtenerValor(String clave);
    void actualizar(String clave, String valor);
}
