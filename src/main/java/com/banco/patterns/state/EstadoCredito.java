package com.banco.patterns.state;

import com.banco.model.EstadoSolicitud;
import com.banco.model.SolicitudCredito;

/**
 * PATRON DE COMPORTAMIENTO: STATE (interfaz Estado).
 * Cada estado de una solicitud de credito es una clase que decide que acciones se permiten.
 * Asi SolicitudCredito (el Contexto) no necesita if/switch encadenados segun su estado:
 * solo delega en el objeto de estado actual.
 *
 * Flujo: PENDIENTE -> EN_EVALUACION -> APROBADA -> DESEMBOLSADA
 *                                   \-> RECHAZADA
 */
public interface EstadoCredito {

    EstadoSolicitud getValor();

    /** El asesor toma la solicitud para revisarla. */
    void evaluar(SolicitudCredito solicitud);

    void aprobar(SolicitudCredito solicitud);

    void rechazar(SolicitudCredito solicitud);

    /** El dinero se abona en la cuenta del cliente. */
    void desembolsar(SolicitudCredito solicitud);

    /** true si el asesor puede tomar una decision en este estado (para mostrar los botones). */
    default boolean admiteDecision() {
        return false;
    }

    /** true si ya no admite ningun cambio. */
    default boolean esFinal() {
        return false;
    }
}
