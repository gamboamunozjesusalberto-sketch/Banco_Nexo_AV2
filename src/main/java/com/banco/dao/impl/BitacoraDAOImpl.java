package com.banco.dao.impl;

import com.banco.dao.BitacoraDAO;
import com.banco.model.RegistroAcceso;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Bitacora de accesos (ingresos, salidas e intentos fallidos) con JDBC. */
public class BitacoraDAOImpl implements BitacoraDAO {

    @Override
    public void registrar(String username, String evento, String detalle) {
        String sql = "INSERT INTO bitacora_acceso (username, evento, detalle) VALUES (?, ?, ?)";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, evento);
            ps.setString(3, detalle);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al registrar en la bitacora: " + e.getMessage(), e);
        }
    }

    @Override
    public List<RegistroAcceso> listarRecientes(int limite) {
        String sql = "SELECT id, username, evento, detalle, fecha FROM bitacora_acceso "
                + "ORDER BY fecha DESC, id DESC LIMIT ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        List<RegistroAcceso> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RegistroAcceso r = new RegistroAcceso();
                    r.setId(rs.getInt("id"));
                    r.setUsername(rs.getString("username"));
                    r.setEvento(rs.getString("evento"));
                    r.setDetalle(rs.getString("detalle"));
                    r.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    lista.add(r);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar la bitacora: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public int contarFallosRecientes(String username, int minutos) {
        String sql = "SELECT COUNT(*) FROM bitacora_acceso b WHERE b.username = ? AND b.evento = 'LOGIN_FALLO' "
                + "AND b.fecha > CURRENT_TIMESTAMP - make_interval(mins => ?) "
                + "AND b.fecha > COALESCE((SELECT MAX(x.fecha) FROM bitacora_acceso x WHERE x.username = ? "
                + "AND x.evento IN ('LOGIN_OK', 'DESBLOQUEO')), TIMESTAMP '1970-01-01')";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setInt(2, minutos);
            ps.setString(3, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al contar intentos fallidos: " + e.getMessage(), e);
        }
    }
}
