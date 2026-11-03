package com.banco.dao.impl;

import com.banco.dao.AlertaRiesgoDAO;
import com.banco.model.AlertaRiesgo;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Alertas de riesgo del auditor con JDBC. */
public class AlertaRiesgoDAOImpl implements AlertaRiesgoDAO {

    @Override
    public void insertar(AlertaRiesgo a) {
        String sql = "INSERT INTO alerta_riesgo (tipo, nivel, detalle, referencia) VALUES (?, ?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, a.getTipo().name());
            ps.setString(2, a.getNivel().name());
            ps.setString(3, a.getDetalle());
            ps.setString(4, a.getReferencia());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    a.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al registrar la alerta: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AlertaRiesgo> listar(boolean soloPendientes, int limite) {
        String sql = "SELECT id, tipo, nivel, detalle, referencia, revisada, fecha FROM alerta_riesgo "
                + (soloPendientes ? "WHERE revisada = FALSE " : "")
                + "ORDER BY fecha DESC, id DESC LIMIT ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        List<AlertaRiesgo> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AlertaRiesgo a = new AlertaRiesgo();
                    a.setId(rs.getInt("id"));
                    a.setTipo(AlertaRiesgo.Tipo.valueOf(rs.getString("tipo")));
                    a.setNivel(AlertaRiesgo.Nivel.valueOf(rs.getString("nivel")));
                    a.setDetalle(rs.getString("detalle"));
                    a.setReferencia(rs.getString("referencia"));
                    a.setRevisada(rs.getBoolean("revisada"));
                    a.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    lista.add(a);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar alertas: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public int contarPendientes() {
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM alerta_riesgo WHERE revisada = FALSE");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error al contar alertas: " + e.getMessage(), e);
        }
    }

    @Override
    public void marcarRevisada(int id) {
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement("UPDATE alerta_riesgo SET revisada = TRUE WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar la alerta: " + e.getMessage(), e);
        }
    }
}
