package com.banco.util;

/** Error tecnico de base de datos (SQL, conexion). */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
