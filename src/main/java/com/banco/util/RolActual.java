package com.banco.util;

import com.banco.model.Rol;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/** Obtiene el rol del usuario que inicio sesion (Spring Security lo guarda como "ROLE_..."). */
public final class RolActual {

    private RolActual() {
    }

    public static Rol de(Authentication auth) {
        if (auth == null) {
            return null;
        }
        for (GrantedAuthority a : auth.getAuthorities()) {
            String nombre = a.getAuthority();
            if (nombre.startsWith("ROLE_")) {
                return Rol.valueOf(nombre.substring(5));
            }
        }
        return null;
    }
}
