package com.banco.dao.impl;

import com.banco.dao.UsuarioDAO;
import com.banco.model.Rol;
import com.banco.model.Usuario;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD de Usuario con JDBC. Guarda solo el hash BCrypt de la contrasena, nunca el texto. */
public class UsuarioDAOImpl implements UsuarioDAO {

    private static final String COLUMNAS = "id, username, password_hash, nombre, rol, activo, debe_cambiar_clave, cliente_id";

    @Override
    public void insertar(Usuario u) {
        String sql = "INSERT INTO usuario (username, password_hash, nombre, rol, activo, cliente_id, debe_cambiar_clave) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getNombre());
            ps.setString(4, u.getRol().name());
            ps.setBoolean(5, u.isActivo());
            if (u.getClienteId() == null) {
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, u.getClienteId());
            }
            ps.setBoolean(7, u.isDebeCambiarClave());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    u.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        List<Usuario> lista = consultar("SELECT " + COLUMNAS + " FROM usuario WHERE username = ?", username, null);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) {
        List<Usuario> lista = consultar("SELECT " + COLUMNAS + " FROM usuario WHERE id = ?", null, id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public List<Usuario> listar() {
        return consultar("SELECT " + COLUMNAS + " FROM usuario ORDER BY rol, username", null, null);
    }

    @Override
    public void cambiarEstado(int id, boolean activo) {
        String sql = "UPDATE usuario SET activo = ? WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al cambiar estado del usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public void cambiarRol(int id, Rol rol) {
        String sql = "UPDATE usuario SET rol = ? WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, rol.name());
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al cambiar rol del usuario: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existeUsername(String username) {
        return buscarPorUsername(username).isPresent();
    }

    @Override
    public int contar() {
        String sql = "SELECT COUNT(*) FROM usuario";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Error al contar usuarios: " + e.getMessage(), e);
        }
    }

    private List<Usuario> consultar(String sql, String texto, Integer entero) {
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Usuario> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (texto != null) {
                ps.setString(1, texto);
            } else if (entero != null) {
                ps.setInt(1, entero);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Usuario u = new Usuario();
                    u.setId(rs.getInt("id"));
                    u.setUsername(rs.getString("username"));
                    u.setPasswordHash(rs.getString("password_hash"));
                    u.setNombre(rs.getString("nombre"));
                    u.setRol(Rol.valueOf(rs.getString("rol")));
                    u.setActivo(rs.getBoolean("activo"));
                    u.setDebeCambiarClave(rs.getBoolean("debe_cambiar_clave"));
                    int clienteId = rs.getInt("cliente_id");
                    u.setClienteId(rs.wasNull() ? null : clienteId);
                    lista.add(u);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar usuarios: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Optional<Usuario> buscarPorClienteId(int clienteId) {
        List<Usuario> lista = consultar("SELECT " + COLUMNAS + " FROM usuario WHERE cliente_id = ?", null, clienteId);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public void cambiarClave(int id, String passwordHash, boolean debeCambiarla) {
        String sql = "UPDATE usuario SET password_hash = ?, debe_cambiar_clave = ? WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setBoolean(2, debeCambiarla);
            ps.setInt(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al cambiar la clave: " + e.getMessage(), e);
        }
    }
}
