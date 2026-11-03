package com.banco.patterns.proxy;

import com.banco.model.Rol;

/** Reportes disponibles y el unico rol que puede generar cada uno. */
public enum TipoReporte {
    GLOBAL(Rol.ADMINISTRADOR),
    RIESGO(Rol.AUDITOR),
    TRANSACCIONES(Rol.AUDITOR),
    BITACORA(Rol.AUDITOR);

    private final Rol rolPermitido;

    TipoReporte(Rol rolPermitido) {
        this.rolPermitido = rolPermitido;
    }

    public Rol getRolPermitido() {
        return rolPermitido;
    }
}
