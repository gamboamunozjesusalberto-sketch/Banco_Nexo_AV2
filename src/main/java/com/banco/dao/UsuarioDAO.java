package com.banco.dao;

import com.banco.model.Rol;
import com.banco.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioDAO {
    void insertar(Usuario usuario);
    Optional<Usuario> buscarPorUsername(String username);
    Optional<Usuario> buscarPorId(int id);
    List<Usuario> listar();
    void cambiarEstado(int id, boolean activo);
    void cambiarRol(int id, Rol rol);
    boolean existeUsername(String username);
    int contar();

    Optional<Usuario> buscarPorClienteId(int clienteId);

    /** Guarda el nuevo hash. Si debeCambiarla es true, el usuario debera cambiarla al ingresar. */
    void cambiarClave(int id, String passwordHash, boolean debeCambiarla);
}
