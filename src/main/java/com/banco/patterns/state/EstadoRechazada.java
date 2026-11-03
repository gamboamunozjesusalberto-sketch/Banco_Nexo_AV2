package com.banco.patterns.state;

import com.banco.model.EstadoSolicitud;

/** PATRON STATE (estado final): el asesor la rechazo con una observacion. */
public class EstadoRechazada extends EstadoBase {

    @Override
    public EstadoSolicitud getValor() {
        return EstadoSolicitud.RECHAZADA;
    }

    @Override
    public boolean esFinal() {
        return true;
    }
}
