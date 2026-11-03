package com.banco.dao;

import com.banco.model.Cliente;

import java.util.List;
import java.util.Optional;

/** SOLID (D): los servicios dependen de esta abstraccion, no de la clase JDBC concreta. */
public interface ClienteDAO {
    void insertar(Cliente cliente);
    List<Cliente> listar();
    Optional<Cliente> buscarPorId(int id);
    boolean existePorDni(String dni);
}
