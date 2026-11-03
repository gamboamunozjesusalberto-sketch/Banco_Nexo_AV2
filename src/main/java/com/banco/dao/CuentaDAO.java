package com.banco.dao;

import com.banco.model.Cuenta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CuentaDAO {
    void insertar(Cuenta cuenta);
    List<Cuenta> listar();
    List<Cuenta> listarPorCliente(int clienteId);
    Optional<Cuenta> buscarPorId(int id);
    Optional<Cuenta> buscarPorNumero(String numero);
    void actualizarSaldo(int id, BigDecimal nuevoSaldo);
    void actualizarLimite(int id, BigDecimal nuevoLimite);
    String siguienteNumero();
}
