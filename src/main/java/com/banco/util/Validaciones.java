package com.banco.util;

import java.math.BigDecimal;

/** Validaciones reutilizables de datos de entrada. */
public final class Validaciones {

    private Validaciones() {
    }

    public static boolean vacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }

    public static boolean dniValido(String dni) {
        return dni != null && dni.matches("\\d{8}");
    }

    public static boolean correoValido(String correo) {
        return correo != null && correo.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    }

    public static boolean telefonoValido(String telefono) {
        return telefono == null || telefono.isBlank() || telefono.matches("\\d{9}");
    }

    public static boolean usernameValido(String username) {
        return username != null && username.matches("[A-Za-z0-9._-]{3,30}");
    }

    /** Regla UNICA de claves en todo el sistema: 8 a 60 caracteres, con al menos una letra y un numero. */
    public static boolean passwordSegura(String password) {
        return password != null && password.length() >= 8 && password.length() <= 60
                && password.matches(".*[A-Za-z].*") && password.matches(".*\\d.*");
    }

    public static boolean montoValido(BigDecimal monto) {
        return monto != null && monto.signum() > 0 && monto.scale() <= 2;
    }
}
