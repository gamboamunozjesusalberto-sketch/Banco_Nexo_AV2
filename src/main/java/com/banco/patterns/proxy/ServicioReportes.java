package com.banco.patterns.proxy;

import com.banco.model.Rol;

/**
 * PATRON ESTRUCTURAL: PROXY (interfaz comun).
 * Tanto el servicio real como el proxy la implementan; por eso los controladores no saben
 * si estan hablando con uno o con otro.
 */
public interface ServicioReportes {

    ArchivoReporte generar(TipoReporte tipo, String formato, String usuario, Rol rol);
}
