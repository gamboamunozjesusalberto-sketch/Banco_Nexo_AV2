package com.banco.dao.impl;

import com.banco.dao.CuentaDAO;
import com.banco.model.Cuenta;
import com.banco.model.TipoCuenta;
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

/** CRUD de Cuenta con JDBC. */
public class CuentaDAOImpl implements CuentaDAO {

    private static final String SELECT_BASE =
            "SELECT c.id, c.numero, c.tipo, c.saldo, c.limite_operacion, c.cliente_id, c.fecha_apertura, "
                    + "cl.nombre AS cliente_nombre FROM cuenta c JOIN cliente cl ON cl.id = c.cliente_id ";

    @Override
    public void insertar(Cuenta cuenta) {
        String sql = "INSERT INTO cuenta (numero, tipo, saldo, limite_operacion, cliente_id) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING id";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, cuenta.getNumero());
            ps.setString(2, cuenta.getTipo().name());
            ps.setBigDecimal(3, cuenta.getSaldo());
            ps.setBigDecimal(4, cuenta.getLimiteOperacion());
            ps.setInt(5, cuenta.getClienteId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cuenta.setId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al insertar cuenta: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Cuenta> listar() {
        return consultarLista(SELECT_BASE + "ORDER BY c.id", null);
    }

    @Override
    public List<Cuenta> listarPorCliente(int clienteId) {
        return consultarLista(SELECT_BASE + "WHERE c.cliente_id = ? ORDER BY c.id", clienteId);
    }

    @Override
    public Optional<Cuenta> buscarPorId(int id) {
        List<Cuenta> lista = consultarLista(SELECT_BASE + "WHERE c.id = ?", id);
        return lista.isEmpty() ? Optional.empty() : Optional.of(lista.get(0));
    }

    @Override
    public Optional<Cuenta> buscarPorNumero(String numero) {
        String sql = SELECT_BASE + "WHERE c.numero = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, numero);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al buscar cuenta: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizarSaldo(int id, BigDecimal nuevoSaldo) {
        actualizarDecimal("UPDATE cuenta SET saldo = ? WHERE id = ?", nuevoSaldo, id);
    }

    @Override
    public void actualizarLimite(int id, BigDecimal nuevoLimite) {
        actualizarDecimal("UPDATE cuenta SET limite_operacion = ? WHERE id = ?", nuevoLimite, id);
    }

    @Override
    public String siguienteNumero() {
        String sql = "SELECT nextval('seq_numero_cuenta')";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return "NX-" + rs.getLong(1);
        } catch (SQLException e) {
            throw new DatabaseException("Error al generar numero de cuenta: " + e.getMessage(), e);
        }
    }

    private void actualizarDecimal(String sql, BigDecimal valor, int id) {
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, valor);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar cuenta: " + e.getMessage(), e);
        }
    }

    /** Ejecuta la consulta; si parametro no es null se asigna como unico parametro entero. */
    private List<Cuenta> consultarLista(String sql, Integer parametro) {
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Cuenta> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al consultar cuentas: " + e.getMessage(), e);
        }
        return lista;
    }

    private Cuenta mapear(ResultSet rs) throws SQLException {
        Cuenta c = Cuenta.crear(TipoCuenta.valueOf(rs.getString("tipo")));
        c.setId(rs.getInt("id"));
        c.setNumero(rs.getString("numero"));
        c.setSaldo(rs.getBigDecimal("saldo"));
        c.setLimiteOperacion(rs.getBigDecimal("limite_operacion"));
        c.setClienteId(rs.getInt("cliente_id"));
        c.setClienteNombre(rs.getString("cliente_nombre"));
        c.setFechaApertura(rs.getTimestamp("fecha_apertura").toLocalDateTime());
        return c;
    }
}
