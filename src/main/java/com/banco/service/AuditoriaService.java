package com.banco.service;

import com.banco.dao.AlertaRiesgoDAO;
import com.banco.dao.BitacoraDAO;
import com.banco.dao.TransaccionDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.AlertaRiesgo;
import com.banco.model.RegistroAcceso;
import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;
import com.banco.patterns.observer.EventoBancario;
import com.banco.patterns.observer.PublicadorEventos;
import org.springframework.stereotype.Service;

import java.util.List;

/** Consultas del auditor: transacciones, bitacora de accesos y alertas de riesgo. */
@Service
public class AuditoriaService {

    private static final int REGISTROS_BITACORA = 200;
    private static final int ALERTAS_VISIBLES = 200;

    private final BitacoraDAO bitacoraDAO = DAOFactory.getBitacoraDAO();
    private final TransaccionDAO transaccionDAO = DAOFactory.getTransaccionDAO();
    private final AlertaRiesgoDAO alertaDAO = DAOFactory.getAlertaRiesgoDAO();
    private final PublicadorEventos eventos;

    public AuditoriaService(PublicadorEventos eventos) {
        this.eventos = eventos;
    }

    /** Registra un ingreso, salida o intento fallido. Un fallo de la bitacora no debe impedir el acceso. */
    public void registrarAcceso(String username, String evento, String detalle) {
        try {
            bitacoraDAO.registrar(username, evento, detalle);
            if ("LOGIN_FALLO".equals(evento)) {
                // PATRON OBSERVER: el monitor de riesgo cuenta los intentos fallidos
                eventos.publicar(new EventoBancario(EventoBancario.Tipo.LOGIN_FALLIDO).usuario(username));
            }
        } catch (RuntimeException ignorada) {
            // la bitacora es de apoyo; no debe bloquear el login
        }
    }

    public List<RegistroAcceso> bitacora() {
        return bitacoraDAO.listarRecientes(REGISTROS_BITACORA);
    }

    /** Ultimas transacciones; si tipo es null, de todos los tipos. */
    public List<Transaccion> transacciones(TipoTransaccion tipo) {
        return transaccionDAO.listarTodas(tipo);
    }

    /** Reporte global (administrador): cantidad y monto por tipo de movimiento. */
    public List<Object[]> resumenPorTipo() {
        return transaccionDAO.resumenPorTipo();
    }

    /** Reporte global: cuanto se recaudo por cada servicio (lo que el banco luego liquida a cada empresa). */
    public List<Object[]> recaudadoPorServicio() {
        return transaccionDAO.recaudadoPorServicio();
    }

    /** Reporte global (administrador): monto movido por dia. */
    public List<Object[]> resumenPorDia(int dias) {
        return transaccionDAO.resumenPorDia(dias);
    }

    public List<AlertaRiesgo> alertas(boolean soloPendientes) {
        return alertaDAO.listar(soloPendientes, ALERTAS_VISIBLES);
    }

    public int alertasPendientes() {
        return alertaDAO.contarPendientes();
    }

    public void marcarAlertaRevisada(int id, String auditor) {
        alertaDAO.marcarRevisada(id);
        registrarAcceso(auditor, "ALERTA_REVISADA", "Alerta #" + id);
    }
}
