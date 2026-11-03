package com.banco.patterns.state;

import com.banco.model.SolicitudCredito;
import com.banco.util.NegocioException;

/**
 * PATRON STATE: clase base con el comportamiento comun.
 * Por defecto ninguna accion esta permitida; cada estado concreto sobrescribe solo las que si admite.
 * Esto evita repetir el mismo "no se puede" en cada clase.
 */
public abstract class EstadoBase implements EstadoCredito {

    @Override
    public void evaluar(SolicitudCredito solicitud) {
        throw noPermitido("pasar a evaluacion");
    }

    @Override
    public void aprobar(SolicitudCredito solicitud) {
        throw noPermitido("aprobarse");
    }

    @Override
    public void rechazar(SolicitudCredito solicitud) {
        throw noPermitido("rechazarse");
    }

    @Override
    public void desembolsar(SolicitudCredito solicitud) {
        throw noPermitido("desembolsarse");
    }

    private NegocioException noPermitido(String accion) {
        return new NegocioException("Una solicitud en estado " + getValor().getNombre().toUpperCase()
                + " no puede " + accion);
    }
}
