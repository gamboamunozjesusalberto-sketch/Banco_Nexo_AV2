package com.banco.patterns.observer;

import com.banco.dao.NotificacionDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Notificacion;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PATRON OBSERVER (observador concreto): deja un aviso en la campana del cliente cuando
 * recibe una transferencia o cuando se resuelve su credito.
 */
@Component
public class NotificadorCliente implements ObservadorBancario {

    private final NotificacionDAO notificacionDAO = DAOFactory.getNotificacionDAO();

    @Override
    public void actualizar(EventoBancario e) {
        if (e.getClienteId() == null) {
            return;
        }
        switch (e.getTipo()) {
            case TRANSFERENCIA_RECIBIDA -> avisar(e.getClienteId(), "Recibiste una transferencia",
                    "Te abonaron " + soles(e.getMonto()) + " en tu cuenta " + e.getCuentaNumero() + ". " + texto(e));
            case CREDITO_DESEMBOLSADO -> avisar(e.getClienteId(), "Tu credito fue aprobado",
                    "Abonamos " + soles(e.getMonto()) + " en tu cuenta " + e.getCuentaNumero()
                            + ". Revisa tu cronograma de pagos en Creditos.");
            case CREDITO_RECHAZADO -> avisar(e.getClienteId(), "Tu solicitud de credito fue rechazada",
                    "Motivo del asesor: " + texto(e));
            default -> { /* los demas eventos no le interesan al cliente */ }
        }
    }

    private void avisar(int clienteId, String titulo, String mensaje) {
        Notificacion n = new Notificacion();
        n.setClienteId(clienteId);
        n.setTitulo(titulo);
        n.setMensaje(mensaje.length() > 250 ? mensaje.substring(0, 250) : mensaje);
        notificacionDAO.insertar(n);
    }

    private static String soles(BigDecimal monto) {
        return monto == null ? "" : "S/ " + monto.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String texto(EventoBancario e) {
        return e.getDetalle() == null ? "" : e.getDetalle();
    }
}
