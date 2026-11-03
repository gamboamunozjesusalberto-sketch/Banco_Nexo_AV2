package com.banco.model;

import com.banco.patterns.state.EstadoAprobada;
import com.banco.patterns.state.EstadoCredito;
import com.banco.patterns.state.EstadoDesembolsada;
import com.banco.patterns.state.EstadoEnEvaluacion;
import com.banco.patterns.state.EstadoPendiente;
import com.banco.patterns.state.EstadoRechazada;

/**
 * Nombre de cada estado tal como se guarda en la base de datos (columna estado de solicitud_credito).
 * El COMPORTAMIENTO de cada estado no esta aqui: esta en las clases del patron State (paquete patterns.state).
 */
public enum EstadoSolicitud {
    PENDIENTE("Pendiente"),
    EN_EVALUACION("En evaluacion"),
    APROBADA("Aprobada"),
    DESEMBOLSADA("Desembolsada"),
    RECHAZADA("Rechazada");

    private final String nombre;

    EstadoSolicitud(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /** Devuelve el objeto de estado (patron State) que corresponde a este valor guardado. */
    public EstadoCredito comoEstado() {
        return switch (this) {
            case PENDIENTE -> new EstadoPendiente();
            case EN_EVALUACION -> new EstadoEnEvaluacion();
            case APROBADA -> new EstadoAprobada();
            case DESEMBOLSADA -> new EstadoDesembolsada();
            case RECHAZADA -> new EstadoRechazada();
        };
    }
}
