package com.banco.service;

import com.banco.dao.ClienteDAO;
import com.banco.dao.CuentaDAO;
import com.banco.dao.SolicitudCreditoDAO;
import com.banco.dao.TransaccionDAO;
import com.banco.dao.UsuarioDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Cliente;
import com.banco.model.Cuenta;
import com.banco.model.HistorialCliente;
import com.banco.model.SolicitudCredito;
import com.banco.model.Transaccion;
import com.banco.model.Usuario;
import com.banco.patterns.observer.EventoBancario;
import com.banco.patterns.observer.PublicadorEventos;
import com.banco.util.NegocioException;
import com.banco.util.TransaccionJdbc;
import com.banco.util.Validaciones;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Solicitudes de credito: el cliente las registra y el asesor las evalua.
 * PATRON STATE: los cambios de estado los decide cada objeto de estado (paquete patterns.state).
 * Al aprobar, el credito se DESEMBOLSA en la cuenta que eligio el cliente (como Interbank y Nubank,
 * que abonan el prestamo directo en la cuenta), todo dentro de una transaccion JDBC.
 * PATRON OBSERVER: el resultado se publica y NotificadorCliente avisa al cliente.
 */
@Service
public class CreditoService {

    private static final int PLAZO_MAXIMO_MESES = 60;
    private static final int LARGO_MAXIMO_MOTIVO = 200;
    private static final int LARGO_MAXIMO_OBSERVACION = 250;

    private final SolicitudCreditoDAO solicitudDAO = DAOFactory.getSolicitudCreditoDAO();
    private final UsuarioDAO usuarioDAO = DAOFactory.getUsuarioDAO();
    private final ClienteDAO clienteDAO = DAOFactory.getClienteDAO();
    private final CuentaDAO cuentaDAO = DAOFactory.getCuentaDAO();
    private final TransaccionDAO transaccionDAO = DAOFactory.getTransaccionDAO();
    private final ParametroService parametroService;
    private final CuentaService cuentaService;
    private final PublicadorEventos eventos;

    public CreditoService(ParametroService parametroService, CuentaService cuentaService, PublicadorEventos eventos) {
        this.parametroService = parametroService;
        this.cuentaService = cuentaService;
        this.eventos = eventos;
    }

    /** Tasa anual vigente (parametro), para la simulacion de cuotas en pantalla. */
    public BigDecimal tasaVigente() {
        return parametroService.obtenerDecimal(ParametroService.TASA_INTERES_ANUAL, new BigDecimal("18"));
    }

    public SolicitudCredito solicitar(int clienteId, BigDecimal monto, int plazoMeses, String motivo, Integer cuentaDestinoId) {
        if (cuentaDestinoId == null) {
            throw new NegocioException("Elige la cuenta donde quieres recibir el dinero");
        }
        cuentaService.buscarDeCliente(cuentaDestinoId, clienteId); // la cuenta debe ser suya
        BigDecimal montoMaximo = parametroService.obtenerDecimal(ParametroService.MONTO_MAXIMO_CREDITO,
                new BigDecimal("100000"));
        if (!Validaciones.montoValido(monto) || monto.compareTo(montoMaximo) > 0) {
            throw new NegocioException("El monto debe ser mayor a 0 y no superar S/ " + montoMaximo.toPlainString());
        }
        if (plazoMeses < 1 || plazoMeses > PLAZO_MAXIMO_MESES) {
            throw new NegocioException("El plazo debe estar entre 1 y " + PLAZO_MAXIMO_MESES + " meses");
        }
        if (Validaciones.vacio(motivo) || motivo.trim().length() > LARGO_MAXIMO_MOTIVO) {
            throw new NegocioException("Indica el motivo del credito (maximo " + LARGO_MAXIMO_MOTIVO + " caracteres)");
        }
        SolicitudCredito s = new SolicitudCredito();
        s.setClienteId(clienteId);
        s.setMonto(monto);
        s.setPlazoMeses(plazoMeses);
        s.setMotivo(motivo.trim());
        s.setCuentaDestinoId(cuentaDestinoId);
        s.setTasaAnual(tasaVigente()); // la tasa queda fija desde que se solicita
        solicitudDAO.insertar(s);
        return s;
    }

    public List<SolicitudCredito> listarTodas() {
        return solicitudDAO.listarTodas();
    }

    public List<SolicitudCredito> listarPorCliente(int clienteId) {
        return solicitudDAO.listarPorCliente(clienteId);
    }

    public SolicitudCredito buscar(int id) {
        return solicitudDAO.buscarPorId(id).orElseThrow(() -> new NegocioException("La solicitud no existe"));
    }

    /** El asesor toma la solicitud para revisarla (PENDIENTE -> EN_EVALUACION). */
    public void iniciarEvaluacion(int solicitudId, String usernameAsesor) {
        SolicitudCredito s = buscar(solicitudId);
        s.evaluar(); // PATRON STATE: si el estado no lo permite, el propio estado lanza el error
        s.setAsesorId(idDe(usernameAsesor));
        solicitudDAO.actualizarResolucion(s);
    }

    /**
     * El asesor aprueba o rechaza. Rechazar exige una observacion.
     * Aprobar = APROBADA + desembolso en la cuenta del cliente -> DESEMBOLSADA, en una sola transaccion:
     * si el abono falla, la solicitud tampoco cambia (rollback).
     */
    public void resolver(int solicitudId, boolean aprobar, String observaciones, String usernameAsesor) {
        String obs = Validaciones.vacio(observaciones) ? null : observaciones.trim();
        if (!aprobar && obs == null) {
            throw new NegocioException("Para rechazar una solicitud debes registrar una observacion");
        }
        if (obs != null && obs.length() > LARGO_MAXIMO_OBSERVACION) {
            throw new NegocioException("La observacion no puede superar " + LARGO_MAXIMO_OBSERVACION + " caracteres");
        }
        TransaccionJdbc.ejecutar(() -> {
            SolicitudCredito s = buscar(solicitudId);
            s.setObservaciones(obs);
            s.setAsesorId(idDe(usernameAsesor));
            if (aprobar) {
                s.aprobar();      // EN_EVALUACION -> APROBADA
                cuentaService.desembolsar(s.getCuentaDestinoId(), s.getClienteId(), s.getMonto(),
                        "Desembolso del credito #" + s.getId() + " (" + s.getPlazoMeses() + " meses)");
                s.desembolsar();  // APROBADA -> DESEMBOLSADA
                solicitudDAO.actualizarResolucion(s);
                eventos.publicar(new EventoBancario(EventoBancario.Tipo.CREDITO_DESEMBOLSADO)
                        .cliente(s.getClienteId()).cuenta(s.getCuentaDestinoId(), s.getCuentaDestinoNumero())
                        .monto(s.getMonto()));
            } else {
                s.rechazar();     // EN_EVALUACION -> RECHAZADA
                solicitudDAO.actualizarResolucion(s);
                eventos.publicar(new EventoBancario(EventoBancario.Tipo.CREDITO_RECHAZADO)
                        .cliente(s.getClienteId()).monto(s.getMonto()).detalle(obs));
            }
            return null;
        });
    }

    private Integer idDe(String username) {
        return usuarioDAO.buscarPorUsername(username).map(Usuario::getId).orElse(null);
    }

    /** Cliente, cuentas, movimientos y solicitudes de credito, para que el asesor evalue. */
    public HistorialCliente historialDeCliente(int clienteId) {
        Cliente cliente = clienteDAO.buscarPorId(clienteId)
                .orElseThrow(() -> new NegocioException("El cliente no existe"));
        List<Cuenta> cuentas = cuentaDAO.listarPorCliente(clienteId);
        List<Transaccion> movimientos = new ArrayList<>();
        for (Cuenta cuenta : cuentas) {
            for (Transaccion t : transaccionDAO.listarPorCuenta(cuenta.getId())) {
                t.setCuentaNumero(cuenta.getNumero());
                movimientos.add(t);
            }
        }
        movimientos.sort(Comparator.comparing(Transaccion::getFecha).reversed());
        return new HistorialCliente(cliente, cuentas, movimientos, solicitudDAO.listarPorCliente(clienteId));
    }
}
