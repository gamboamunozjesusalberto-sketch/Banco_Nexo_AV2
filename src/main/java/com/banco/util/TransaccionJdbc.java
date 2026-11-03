package com.banco.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Ejecuta un bloque de trabajo dentro de una TRANSACCION JDBC: commit si todo sale bien,
 * rollback si algo falla. Si ya hay una transaccion abierta (por ejemplo, la abrio el
 * servicio que llamo a este), el bloque se une a ella en lugar de abrir otra.
 */
public final class TransaccionJdbc {

    private TransaccionJdbc() {
    }

    public static <T> T ejecutar(Supplier<T> accion) {
        // Cada peticion tiene su propia conexion del pool: varias transacciones pueden ocurrir a la vez.
        Connection con = ConexionBD.getInstancia().getConexion();
        boolean propia = false;
        try {
            if (con.getAutoCommit()) {
                con.setAutoCommit(false);
                propia = true;
            }
            T resultado = accion.get();
            if (propia) {
                con.commit();
            }
            return resultado;
        } catch (RuntimeException e) {
            if (propia) {
                revertir(con);
            }
            throw e;
        } catch (SQLException e) {
            if (propia) {
                revertir(con);
            }
            throw new DatabaseException("Error en la transaccion: " + e.getMessage(), e);
        } finally {
            if (propia) {
                restaurarAutoCommit(con);
            }
        }
    }

    private static void revertir(Connection con) {
        try {
            con.rollback();
        } catch (SQLException ignorada) {
            // si falla el rollback no hay mas que hacer
        }
    }

    private static void restaurarAutoCommit(Connection con) {
        try {
            con.setAutoCommit(true);
        } catch (SQLException ignorada) {
            // la conexion se reabrira en el siguiente uso si quedo invalida
        }
    }
}
