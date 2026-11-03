package com.banco.dao.impl;

import com.banco.dao.TransaccionDAO;
import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** CRUD de Transaccion con JDBC. */
public class TransaccionDAOImpl implements TransaccionDAO {

    private static final String SELECT_BASE =
            "SELECT t.id, t.cuenta_id, t.tipo, t.monto, t.saldo_resultante, t.fecha, t.descripcion, "
                    + "c.numero AS cuenta_numero, cl.nombre AS cliente_nombre "
                    + "FROM transaccion t JOIN cuenta c ON c.id = t.cuenta_id "
                    + "JOIN cliente cl ON cl.id = c.cliente_id ";

    @Override
    public void insertar(Transaccion t) {
        String sql = "INSERT INTO transaccion (cuenta_id, tipo, monto, saldo_resultante, descripcion) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, t.getCuentaId());
            ps.setString(2, t.getTipo().name());
            ps.setBigDecimal(3, t.getMonto());
            ps.setBigDecimal(4, t.getSaldoResultante());
            ps.setString(5, t.getDescripcion());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    t.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al registrar transaccion: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaccion> listarPorCuenta(int cuentaId) {
        return consultar(SELECT_BASE + "WHERE t.cuenta_id = ? ORDER BY t.fecha DESC, t.id DESC", cuentaId, null);
    }

    @Override
    public Optional<Transaccion> buscarPorId(int id) {
        List<Transaccion> lista = consultar(SELECT_BASE + "WHERE t.id = ?", id, null);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public List<Transaccion> listarTodas(TipoTransaccion tipo) {
        if (tipo == null) {
            return consultar(SELECT_BASE + "ORDER BY t.fecha DESC, t.id DESC LIMIT 300", null, null);
        }
        return consultar(SELECT_BASE + "WHERE t.tipo = ? ORDER BY t.fecha DESC, t.id DESC LIMIT 300",
                null, tipo.name());
    }

    /** Solo uno de los dos parametros (entero o texto) viene informado; el otro es null. */
    private List<Transaccion> consultar(String sql, Integer entero, String texto) {
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Transaccion> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (entero != null) {
                ps.setInt(1, entero);
            } else if (texto != null) {
                ps.setString(1, texto);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Transaccion t = new Transaccion();
                    t.setId(rs.getInt("id"));
                    t.setCuentaId(rs.getInt("cuenta_id"));
                    t.setTipo(TipoTransaccion.valueOf(rs.getString("tipo")));
                    t.setMonto(rs.getBigDecimal("monto"));
                    t.setSaldoResultante(rs.getBigDecimal("saldo_resultante"));
                    t.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                    t.setDescripcion(rs.getString("descripcion"));
                    t.setCuentaNumero(rs.getString("cuenta_numero"));
                    t.setClienteNombre(rs.getString("cliente_nombre"));
                    lista.add(t);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar transacciones: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public int contarRecientesPorCuenta(int cuentaId, int minutos) {
        String sql = "SELECT COUNT(*) FROM transaccion WHERE cuenta_id = ? "
                + "AND fecha > CURRENT_TIMESTAMP - make_interval(mins => ?)";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cuentaId);
            ps.setInt(2, minutos);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al contar movimientos: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Object[]> resumenPorTipo() {
        String sql = "SELECT tipo, COUNT(*) AS cantidad, COALESCE(SUM(monto), 0) AS total "
                + "FROM transaccion WHERE tipo <> 'RECAUDACION_SERVICIO' GROUP BY tipo ORDER BY total DESC";
        return resumen(sql, null);
    }

    @Override
    public List<Object[]> recaudadoPorServicio() {
        // la descripcion del ingreso empieza con el nombre del servicio: "Luz del Sur - cuenta NX-... - operacion ..."
        String sql = "SELECT SPLIT_PART(descripcion, ' - ', 1) AS servicio, COUNT(*) AS cantidad, "
                + "COALESCE(SUM(monto), 0) AS total FROM transaccion WHERE tipo = 'RECAUDACION_SERVICIO' "
                + "GROUP BY 1 ORDER BY total DESC";
        return resumen(sql, null);
    }

    @Override
    public List<Object[]> resumenPorDia(int dias) {
        String sql = "SELECT TO_CHAR(DATE(fecha), 'DD/MM') AS dia, COUNT(*) AS cantidad, COALESCE(SUM(monto), 0) AS total "
                + "FROM transaccion WHERE fecha >= CURRENT_DATE - (? - 1) AND tipo NOT IN ('RECAUDACION_SERVICIO', 'TRANSFERENCIA_RECIBIDA') "
                + "GROUP BY DATE(fecha) ORDER BY DATE(fecha)";
        return resumen(sql, dias);
    }

    private List<Object[]> resumen(String sql, Integer parametro) {
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Object[]> filas = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    filas.add(new Object[]{rs.getString(1), rs.getInt(2), rs.getBigDecimal(3)});
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al generar el resumen: " + e.getMessage(), e);
        }
        return filas;
    }
}
