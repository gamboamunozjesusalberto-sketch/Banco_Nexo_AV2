package com.banco.patterns.state;

import com.banco.model.EstadoSolicitud;
import com.banco.model.SolicitudCredito;

/** PATRON STATE (estado concreto): aprobada; lo unico que falta es abonar el dinero. */
public class EstadoAprobada extends EstadoBase {

    @Override
    public EstadoSolicitud getValor() {
        return EstadoSolicitud.APROBADA;
    }

    @Override
    public void desembolsar(SolicitudCredito solicitud) {
        solicitud.cambiarEstado(new EstadoDesembolsada());
    }
}
