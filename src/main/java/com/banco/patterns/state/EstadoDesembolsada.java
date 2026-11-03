package com.banco.patterns.state;

import com.banco.model.EstadoSolicitud;

/** PATRON STATE (estado final): el dinero ya esta en la cuenta del cliente. */
public class EstadoDesembolsada extends EstadoBase {

    @Override
    public EstadoSolicitud getValor() {
        return EstadoSolicitud.DESEMBOLSADA;
    }

    @Override
    public boolean esFinal() {
        return true;
    }
}
