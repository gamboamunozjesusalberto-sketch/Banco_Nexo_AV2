package com.banco.dao.impl;

import com.banco.dao.ParametroDAO;
import com.banco.model.Parametro;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Parametros de configuracion del sistema guardados en la base de datos. */
public class ParametroDAOImpl implements ParametroDAO {

    @Override
    public List<Parametro> listar() {
        String sql = "SELECT clave, valor, descripcion FROM parametro ORDER BY clave";
        Connection con = ConexionBD.getInstancia().getConexion();
        List<Parametro> lista = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Parametro p = new Parametro();
                p.setClave(rs.getString("clave"));
                p.setValor(rs.getString("valor"));
                p.setDescripcion(rs.getString("descripcion"));
                lista.add(p);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al listar parametros: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public Optional<String> obtenerValor(String clave) {
        String sql = "SELECT valor FROM parametro WHERE clave = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, clave);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error al leer el parametro: " + e.getMessage(), e);
        }
    }

    @Override
    public void actualizar(String clave, String valor) {
        String sql = "UPDATE parametro SET valor = ? WHERE clave = ?";
        Connection con = ConexionBD.getInstancia().getConexion();
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, valor);
            ps.setString(2, clave);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Error al actualizar el parametro: " + e.getMessage(), e);
        }
    }
}
