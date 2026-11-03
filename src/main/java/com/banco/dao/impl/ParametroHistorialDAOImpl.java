package com.banco.dao.impl;

import com.banco.dao.ParametroHistorialDAO;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Historial de versiones de parametros con JDBC. */
public class ParametroHistorialDAOImpl implements ParametroHistorialDAO {

    private static final String SELECT_BASE = "SELECT id, valores, usuario, descripcion, fecha FROM parametro_historial ";

    @Override
    public int insertar(String valores, String usuario, String descripcion) {
        String sql = "INSERT INTO parametro_historial (valores, usuario, descripcion) VALUES (?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valores);
            ps.setString(2, usuario);
            ps.setString(3, descripcion.length() > 150 ? descripcion.substring(0, 150) : descripcion);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al guardar la version de parametros: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Registro> listar(int limite) {
        return consultar(SELECT_BASE + "ORDER BY fecha DESC, id DESC LIMIT ?", limite);
    }

    @Override
    public Optional<Registro> buscarPorId(int id) {
        List<Registro> lista = consultar(SELECT_BASE + "WHERE id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    private List<Registro> consultar(String sql, int parametro) {
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Registro> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Registro(rs.getInt("id"), rs.getString("valores"), rs.getString("usuario"),
                            rs.getString("descripcion"), rs.getTimestamp("fecha").toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar el historial de parametros: " + e.getMessage(), e);
        }
        return lista;
    }
}
