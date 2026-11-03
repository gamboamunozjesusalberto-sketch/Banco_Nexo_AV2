package com.banco.dao.impl;

import com.banco.dao.OperacionCajaDAO;
import com.banco.model.OperacionCaja;
import com.banco.model.TipoTransaccion;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;
import com.banco.util.NegocioException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Historial de ordenes de caja (patron Command) con JDBC. */
public class OperacionCajaDAOImpl implements OperacionCajaDAO {

    private static final String SELECT_BASE =
            "SELECT o.id, o.comando, o.cuenta_id, c.numero AS cuenta_numero, cl.nombre AS cliente_nombre, o.monto, "
                    + "o.cajero, o.transaccion_id, o.estado, o.extorno_id, o.extornado_por, o.motivo_extorno, o.fecha "
                    + "FROM operacion_caja o JOIN cuenta c ON c.id = o.cuenta_id JOIN cliente cl ON cl.id = c.cliente_id ";

    @Override
    public void insertar(OperacionCaja o) {
        String sql = "INSERT INTO operacion_caja (comando, cuenta_id, monto, cajero, transaccion_id, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, o.getComando().name());
            ps.setInt(2, o.getCuentaId());
            ps.setBigDecimal(3, o.getMonto());
            ps.setString(4, o.getCajero());
            ps.setObject(5, o.getTransaccionId(), Types.INTEGER);
            ps.setString(6, o.getEstado().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    o.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al registrar la operacion de caja: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OperacionCaja> listar(int limite) {
        return consultar(SELECT_BASE + "ORDER BY o.fecha DESC, o.id DESC LIMIT ?", limite);
    }

    @Override
    public Optional<OperacionCaja> buscarPorId(int id) {
        List<OperacionCaja> lista = consultar(SELECT_BASE + "WHERE o.id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public void marcarExtornada(int id, int extornoId, String extornadoPor, String motivo) {
        String sql = "UPDATE operacion_caja SET estado = 'EXTORNADA', extorno_id = ?, extornado_por = ?, "
                + "motivo_extorno = ? WHERE id = ? AND estado = 'EJECUTADA'";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, extornoId);
            ps.setString(2, extornadoPor);
            ps.setString(3, motivo);
            ps.setInt(4, id);
            if (ps.executeUpdate() == 0) {
                throw new NegocioException("La operacion ya fue extornada");
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al extornar la operacion: " + e.getMessage(), e);
        }
    }

    private List<OperacionCaja> consultar(String sql, int parametro) {
        Connection con = ConexionBD.getInstancia().getConexion();
        List<OperacionCaja> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OperacionCaja o = new OperacionCaja();
                    o.setId(rs.getInt("id"));
                    o.setComando(TipoTransaccion.valueOf(rs.getString("comando")));
                    o.setCuentaId(rs.getInt("cuenta_id"));
                    o.setCuentaNumero(rs.getString("cuenta_numero"));
                    o.setClienteNombre(rs.getString("cliente_nombre"));
                    o.setMonto(rs.getBigDecimal("monto"));
                    o.setCajero(rs.getString("cajero"));
                    int t = rs.getInt("transaccion_id");
                    o.setTransaccionId(rs.wasNull() ? null : t);
                    o.setEstado(OperacionCaja.Estado.valueOf(rs.getString("estado")));
                    int ext = rs.getInt("extorno_id");
                    o.setExtornoId(rs.wasNull() ? null : ext);
                    o.setExtornadoPor(rs.getString("extornado_por"));
                    o.setMotivoExtorno(rs.getString("motivo_extorno"));
                    o.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    lista.add(o);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar operaciones de caja: " + e.getMessage(), e);
        }
        return lista;
    }
}
