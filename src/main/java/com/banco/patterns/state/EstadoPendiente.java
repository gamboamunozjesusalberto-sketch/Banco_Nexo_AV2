package com.banco.patterns.state;

import com.banco.model.EstadoSolicitud;
import com.banco.model.SolicitudCredito;

/** PATRON STATE (estado concreto): la solicitud acaba de llegar; solo puede pasar a evaluacion. */
public class EstadoPendiente extends EstadoBase {

    @Override
    public EstadoSolicitud getValor() {
        return EstadoSolicitud.PENDIENTE;
    }

    @Override
    public void evaluar(SolicitudCredito solicitud) {
        solicitud.cambiarEstado(new EstadoEnEvaluacion());
    }
}
