package com.banco.dao.impl;

import com.banco.dao.SolicitudCreditoDAO;
import com.banco.model.SolicitudCredito;
import com.banco.model.EstadoSolicitud;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD de SolicitudCredito con JDBC. */
public class SolicitudCreditoDAOImpl implements SolicitudCreditoDAO {

    private static final String SELECT_BASE =
            "SELECT s.id, s.cliente_id, cl.nombre AS cliente_nombre, s.monto, s.plazo_meses, s.motivo, s.estado, "
                    + "s.observaciones, s.asesor_id, s.cuenta_destino_id, cu.numero AS cuenta_destino_numero, s.tasa_anual, "
                    + "s.fecha_solicitud, s.fecha_resolucion "
                    + "FROM solicitud_credito s JOIN cliente cl ON cl.id = s.cliente_id "
                    + "LEFT JOIN cuenta cu ON cu.id = s.cuenta_destino_id ";

    @Override
    public void insertar(SolicitudCredito s) {
        String sql = "INSERT INTO solicitud_credito (cliente_id, monto, plazo_meses, motivo, estado, cuenta_destino_id, tasa_anual) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getClienteId());
            ps.setBigDecimal(2, s.getMonto());
            ps.setInt(3, s.getPlazoMeses());
            ps.setString(4, s.getMotivo());
            ps.setString(5, s.getEstado().name());
            ps.setObject(6, s.getCuentaDestinoId(), Types.INTEGER);
            ps.setBigDecimal(7, s.getTasaAnual());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    s.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al registrar la solicitud de credito: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SolicitudCredito> listarTodas() {
        return consultar(SELECT_BASE + "ORDER BY s.fecha_solicitud DESC, s.id DESC", null);
    }

    @Override
    public List<SolicitudCredito> listarPorCliente(int clienteId) {
        return consultar(SELECT_BASE + "WHERE s.cliente_id = ? ORDER BY s.fecha_solicitud DESC, s.id DESC", clienteId);
    }

    @Override
    public Optional<SolicitudCredito> buscarPorId(int id) {
        List<SolicitudCredito> lista = consultar(SELECT_BASE + "WHERE s.id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public void actualizarResolucion(SolicitudCredito s) {
        // fecha_resolucion solo se marca cuando la solicitud deja de estar pendiente o en evaluacion
        String sql = "UPDATE solicitud_credito SET estado = ?, observaciones = ?, asesor_id = ?, "
                + "fecha_resolucion = CASE WHEN ? IN ('PENDIENTE', 'EN_EVALUACION') THEN NULL "
                + "ELSE COALESCE(fecha_resolucion, CURRENT_TIMESTAMP) END WHERE id = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getEstado().name());
            ps.setString(2, s.getObservaciones());
            if (s.getAsesorId() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, s.getAsesorId());
            }
            ps.setString(4, s.getEstado().name());
            ps.setInt(5, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar la solicitud: " + e.getMessage(), e);
        }
    }

    private List<SolicitudCredito> consultar(String sql, Integer parametro) {
        Connection con = ConexionBD.getInstancia().getConexion();
        List<SolicitudCredito> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SolicitudCredito s = new SolicitudCredito();
                    s.setId(rs.getInt("id"));
                    s.setClienteId(rs.getInt("cliente_id"));
                    s.setClienteNombre(rs.getString("cliente_nombre"));
                    s.setMonto(rs.getBigDecimal("monto"));
                    s.setPlazoMeses(rs.getInt("plazo_meses"));
                    s.setMotivo(rs.getString("motivo"));
                    s.setEstado(EstadoSolicitud.valueOf(rs.getString("estado")));
                    s.setObservaciones(rs.getString("observaciones"));
                    int asesorId = rs.getInt("asesor_id");
                    s.setAsesorId(rs.wasNull() ? null : asesorId);
                    int cuentaDestino = rs.getInt("cuenta_destino_id");
                    s.setCuentaDestinoId(rs.wasNull() ? null : cuentaDestino);
                    s.setCuentaDestinoNumero(rs.getString("cuenta_destino_numero"));
                    s.setTasaAnual(rs.getBigDecimal("tasa_anual"));
                    s.setFechaSolicitud(rs.getTimestamp("fecha_solicitud").toLocalDateTime());
                    Timestamp resolucion = rs.getTimestamp("fecha_resolucion");
                    LocalDateTime fechaResolucion = resolucion == null ? null : resolucion.toLocalDateTime();
                    s.setFechaResolucion(fechaResolucion);
                    lista.add(s);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar solicitudes: " + e.getMessage(), e);
        }
        return lista;
    }
}
