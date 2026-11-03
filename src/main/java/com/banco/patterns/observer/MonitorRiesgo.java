package com.banco.patterns.observer;

import com.banco.dao.AlertaRiesgoDAO;
import com.banco.dao.BitacoraDAO;
import com.banco.dao.TransaccionDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.AlertaRiesgo;
import com.banco.service.ParametroService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PATRON OBSERVER (observador concreto): vigila los eventos y genera alertas para el auditor.
 * Idea tomada de las alertas de seguridad de Bank of America (antecedente internacional).
 * Reglas (los umbrales son parametros que el administrador configura):
 *  - MONTO_ALTO: un movimiento igual o mayor a UMBRAL_MONTO_ALTO.
 *  - OPERACIONES_SEGUIDAS: una cuenta llega a MAX_OPERACIONES_VENTANA movimientos dentro de VENTANA_MINUTOS.
 *  - INTENTOS_FALLIDOS: un usuario llega a MAX_INTENTOS_FALLIDOS ingresos fallidos dentro de MINUTOS_BLOQUEO
 *    (en ese mismo momento el usuario queda bloqueado temporalmente).
 */
@Component
public class MonitorRiesgo implements ObservadorBancario {

    private final AlertaRiesgoDAO alertaDAO = DAOFactory.getAlertaRiesgoDAO();
    private final TransaccionDAO transaccionDAO = DAOFactory.getTransaccionDAO();
    private final BitacoraDAO bitacoraDAO = DAOFactory.getBitacoraDAO();
    private final ParametroService parametros;

    public MonitorRiesgo(ParametroService parametros) {
        this.parametros = parametros;
    }

    @Override
    public void actualizar(EventoBancario e) {
        switch (e.getTipo()) {
            case MOVIMIENTO_REGISTRADO -> revisarMovimiento(e);
            case LOGIN_FALLIDO -> revisarIntentosFallidos(e);
            default -> { /* otros eventos no generan alertas */ }
        }
    }

    private void revisarMovimiento(EventoBancario e) {
        BigDecimal umbral = parametros.obtenerDecimal(ParametroService.UMBRAL_MONTO_ALTO, new BigDecimal("3000"));
        if (e.getMonto() != null && e.getMonto().compareTo(umbral) >= 0) {
            boolean muyAlto = e.getMonto().compareTo(umbral.multiply(BigDecimal.valueOf(2))) >= 0;
            registrar(AlertaRiesgo.Tipo.MONTO_ALTO, muyAlto ? AlertaRiesgo.Nivel.ALTO : AlertaRiesgo.Nivel.MEDIO,
                    "Movimiento de S/ " + e.getMonto().setScale(2, RoundingMode.HALF_UP).toPlainString()
                            + " en la cuenta " + e.getCuentaNumero() + " (" + e.getDetalle() + ")",
                    e.getCuentaNumero());
        }
        if (e.getCuentaId() != null) {
            int ventana = parametros.obtenerEntero(ParametroService.VENTANA_MINUTOS, 10);
            int maximo = parametros.obtenerEntero(ParametroService.MAX_OPERACIONES_VENTANA, 5);
            int cantidad = transaccionDAO.contarRecientesPorCuenta(e.getCuentaId(), ventana);
            if (cantidad == maximo) { // se avisa una sola vez, justo al llegar al limite
                registrar(AlertaRiesgo.Tipo.OPERACIONES_SEGUIDAS, AlertaRiesgo.Nivel.MEDIO,
                        cantidad + " movimientos en " + ventana + " minutos en la cuenta " + e.getCuentaNumero(),
                        e.getCuentaNumero());
            }
        }
    }

    private void revisarIntentosFallidos(EventoBancario e) {
        int ventana = parametros.obtenerEntero(ParametroService.MINUTOS_BLOQUEO, 15);
        int maximo = parametros.obtenerEntero(ParametroService.MAX_INTENTOS_FALLIDOS, 3);
        int fallos = bitacoraDAO.contarFallosRecientes(e.getUsername(), ventana);
        if (fallos == maximo) {
            registrar(AlertaRiesgo.Tipo.INTENTOS_FALLIDOS, AlertaRiesgo.Nivel.ALTO,
                    fallos + " intentos de ingreso fallidos en " + ventana + " minutos para el usuario "
                            + e.getUsername() + ". El usuario quedo bloqueado temporalmente.",
                    e.getUsername());
        }
    }

    private void registrar(AlertaRiesgo.Tipo tipo, AlertaRiesgo.Nivel nivel, String detalle, String referencia) {
        AlertaRiesgo a = new AlertaRiesgo();
        a.setTipo(tipo);
        a.setNivel(nivel);
        a.setDetalle(detalle.length() > 250 ? detalle.substring(0, 250) : detalle);
        a.setReferencia(referencia == null ? null : (referencia.length() > 40 ? referencia.substring(0, 40) : referencia));
        alertaDAO.insertar(a);
    }
}
