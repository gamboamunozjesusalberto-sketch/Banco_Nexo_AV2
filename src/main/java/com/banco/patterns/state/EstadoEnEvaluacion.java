package com.banco.patterns.state;

import com.banco.model.EstadoSolicitud;
import com.banco.model.SolicitudCredito;
import com.banco.util.NegocioException;

/** PATRON STATE (estado concreto): el asesor la esta revisando; puede aprobarla o rechazarla. */
public class EstadoEnEvaluacion extends EstadoBase {

    @Override
    public EstadoSolicitud getValor() {
        return EstadoSolicitud.EN_EVALUACION;
    }

    @Override
    public void aprobar(SolicitudCredito solicitud) {
        if (solicitud.getCuentaDestinoId() == null) {
            throw new NegocioException("La solicitud no tiene una cuenta para el desembolso");
        }
        solicitud.cambiarEstado(new EstadoAprobada());
    }

    @Override
    public void rechazar(SolicitudCredito solicitud) {
        solicitud.cambiarEstado(new EstadoRechazada());
    }

    @Override
    public boolean admiteDecision() {
        return true;
    }
}
