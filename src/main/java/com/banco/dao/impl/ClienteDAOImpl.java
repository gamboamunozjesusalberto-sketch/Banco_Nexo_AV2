package com.banco.dao.impl;

import com.banco.dao.ClienteDAO;
import com.banco.model.Cliente;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD de Cliente con JDBC. Se usa PreparedStatement para prevenir inyeccion SQL. */
public class ClienteDAOImpl implements ClienteDAO {

    private static final String COLUMNAS = "id, nombre, dni, correo, telefono";

    @Override
    public void insertar(Cliente c) {
        String sql = "INSERT INTO cliente (nombre, dni, correo, telefono) VALUES (?, ?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getDni());
            ps.setString(3, c.getCorreo());
            ps.setString(4, c.getTelefono());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    c.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Cliente> listar() {
        String sql = "SELECT " + COLUMNAS + " FROM cliente ORDER BY nombre";
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Cliente> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar clientes: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        String sql = "SELECT " + COLUMNAS + " FROM cliente WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar cliente: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existePorDni(String dni) {
        String sql = "SELECT 1 FROM cliente WHERE dni = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, dni);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al verificar DNI: " + e.getMessage(), e);
        }
    }

    private Cliente mapear(ResultSet rs) throws SQLException {
        Cliente c = new Cliente();
        c.setId(rs.getInt("id"));
        c.setNombre(rs.getString("nombre"));
        c.setDni(rs.getString("dni"));
        c.setCorreo(rs.getString("correo"));
        c.setTelefono(rs.getString("telefono"));
        return c;
    }
}
