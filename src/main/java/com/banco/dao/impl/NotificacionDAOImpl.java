package com.banco.dao.impl;

import com.banco.dao.NotificacionDAO;
import com.banco.model.Notificacion;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Notificaciones del cliente con JDBC. */
public class NotificacionDAOImpl implements NotificacionDAO {

    @Override
    public void insertar(Notificacion n) {
        String sql = "INSERT INTO notificacion (cliente_id, titulo, mensaje) VALUES (?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, n.getClienteId());
            ps.setString(2, n.getTitulo());
            ps.setString(3, n.getMensaje());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    n.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al registrar la notificacion: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Notificacion> listarPorCliente(int clienteId, int limite) {
        String sql = "SELECT id, cliente_id, titulo, mensaje, leida, fecha FROM notificacion "
                + "WHERE cliente_id = ? ORDER BY fecha DESC, id DESC LIMIT ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Notificacion> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ps.setInt(2, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notificacion n = new Notificacion();
                    n.setId(rs.getInt("id"));
                    n.setClienteId(rs.getInt("cliente_id"));
                    n.setTitulo(rs.getString("titulo"));
                    n.setMensaje(rs.getString("mensaje"));
                    n.setLeida(rs.getBoolean("leida"));
                    n.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    lista.add(n);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar notificaciones: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public int contarNoLeidas(int clienteId) {
        String sql = "SELECT COUNT(*) FROM notificacion WHERE cliente_id = ? AND leida = FALSE";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al contar notificaciones: " + e.getMessage(), e);
        }
    }

    @Override
    public void marcarLeidas(int clienteId) {
        String sql = "UPDATE notificacion SET leida = TRUE WHERE cliente_id = ? AND leida = FALSE";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar notificaciones: " + e.getMessage(), e);
        }
    }
}
