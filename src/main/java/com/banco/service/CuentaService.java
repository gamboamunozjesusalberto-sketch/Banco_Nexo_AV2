package com.banco.service;

import com.banco.dao.ClienteDAO;
import com.banco.dao.CuentaDAO;
import com.banco.dao.TransaccionDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Cuenta;
import com.banco.model.TipoCuenta;
import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;
import com.banco.patterns.adapter.PasarelaPagoServicios;
import com.banco.patterns.observer.EventoBancario;
import com.banco.patterns.observer.PublicadorEventos;
import com.banco.util.NegocioException;
import com.banco.util.TransaccionJdbc;
import com.banco.util.Validaciones;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Capa de LOGICA DE NEGOCIO para cuentas y operaciones.
 * Usa TRANSACCIONES JDBC (TransaccionJdbc): actualizar saldos y registrar movimientos ocurren
 * juntos (commit) o no ocurren (rollback).
 * Los DAO se obtienen de la fabrica (Factory) y el pago de servicios pasa por un adaptador (Adapter).
 */
@Service
public class CuentaService {

    private static final int LARGO_MAXIMO_DESCRIPCION = 150;

    /**
     * Cuenta interna del banco donde se junta lo cobrado por pago de servicios (como la cuenta
     * recaudadora que un banco real liquida luego a Luz del Sur, Sedapal, etc.). Asi el dinero
     * no "desaparece": sale del cliente y entra aqui en la MISMA transaccion.
     */
    public static final String NUMERO_RECAUDADORA = "NX-RECAUDACION";
    public static final String DNI_RECAUDADORA = "00000000";

    private final CuentaDAO cuentaDAO = DAOFactory.getCuentaDAO();
    private final ClienteDAO clienteDAO = DAOFactory.getClienteDAO();
    private final TransaccionDAO transaccionDAO = DAOFactory.getTransaccionDAO();
    private final ParametroService parametroService;
    private final PasarelaPagoServicios pasarelaPagos;
    private final PublicadorEventos eventos;
    private final UsuarioService usuarioService;

    /**
     * SOLID (D): recibe la interfaz PasarelaPagoServicios, no la clase concreta del adaptador.
     * PATRON OBSERVER: cada movimiento se publica en PublicadorEventos; CuentaService no sabe quien escucha.
     */
    public CuentaService(ParametroService parametroService, PasarelaPagoServicios pasarelaPagos,
                         PublicadorEventos eventos, UsuarioService usuarioService) {
        this.parametroService = parametroService;
        this.pasarelaPagos = pasarelaPagos;
        this.eventos = eventos;
        this.usuarioService = usuarioService;
    }

    // ------------------------------------------------------------------ cuentas (cajero)

    public Cuenta abrirCuenta(int clienteId, TipoCuenta tipo, BigDecimal depositoInicial) {
        BigDecimal inicial = depositoInicial == null ? BigDecimal.ZERO : depositoInicial;
        if (inicial.signum() < 0 || inicial.scale() > 2) {
            throw new NegocioException("El deposito inicial no es valido");
        }
        if (tipo == null) {
            throw new NegocioException("Debes elegir el tipo de cuenta");
        }
        clienteDAO.buscarPorId(clienteId)
                .orElseThrow(() -> new NegocioException("El cliente seleccionado no existe"));
        BigDecimal limite = parametroService.obtenerDecimal(ParametroService.LIMITE_OPERACION_DEFECTO,
                new BigDecimal("5000"));

        return TransaccionJdbc.ejecutar(() -> {
            // PATRON FACTORY (metodo de fabrica): Cuenta.crear devuelve CuentaAhorros o CuentaCorriente
            Cuenta nueva = Cuenta.crear(tipo);
            nueva.setNumero(cuentaDAO.siguienteNumero());
            nueva.setClienteId(clienteId);
            nueva.setLimiteOperacion(limite);
            if (inicial.signum() > 0) {
                nueva.depositar(inicial);
            }
            cuentaDAO.insertar(nueva);
            if (inicial.signum() > 0) {
                registrarMovimiento(nueva, TipoTransaccion.DEPOSITO, inicial, "Deposito inicial");
            }
            return nueva;
        });
    }

    /** Cuentas de clientes (la cuenta interna de recaudacion no se muestra en ventanilla). */
    public List<Cuenta> listar() {
        return cuentaDAO.listar().stream().filter(c -> !esRecaudadora(c)).toList();
    }

    public static boolean esRecaudadora(Cuenta cuenta) {
        return NUMERO_RECAUDADORA.equals(cuenta.getNumero());
    }

    /** Saldo acumulado en la cuenta NEXO Recaudacion (lo muestra el reporte global del administrador). */
    public BigDecimal saldoRecaudado() {
        return cuentaDAO.buscarPorNumero(NUMERO_RECAUDADORA).map(Cuenta::getSaldo).orElse(BigDecimal.ZERO);
    }

    /** Crea la cuenta interna de recaudacion si todavia no existe (se llama al arrancar). */
    public void asegurarCuentaRecaudadora() {
        if (cuentaDAO.buscarPorNumero(NUMERO_RECAUDADORA).isPresent()) {
            return;
        }
        TransaccionJdbc.ejecutar(() -> {
            com.banco.model.Cliente banco = clienteDAO.listar().stream()
                    .filter(c -> DNI_RECAUDADORA.equals(c.getDni())).findFirst().orElse(null);
            if (banco == null) {
                banco = new com.banco.model.Cliente();
                banco.setNombre("Banco NEXO - Recaudacion de servicios");
                banco.setDni(DNI_RECAUDADORA);
                banco.setCorreo("recaudacion@banconexo.pe");
                clienteDAO.insertar(banco);
            }
            Cuenta cuenta = Cuenta.crear(TipoCuenta.CORRIENTE);
            cuenta.setNumero(NUMERO_RECAUDADORA);
            cuenta.setClienteId(banco.getId());
            cuenta.setLimiteOperacion(new BigDecimal("999999999.99"));
            cuentaDAO.insertar(cuenta);
            return cuenta;
        });
    }

    public List<Cuenta> listarPorCliente(int clienteId) {
        return cuentaDAO.listarPorCliente(clienteId);
    }

    public Cuenta buscar(int cuentaId) {
        return cuentaDAO.buscarPorId(cuentaId).orElseThrow(() -> new NegocioException("La cuenta no existe"));
    }

    /** Busca la cuenta y comprueba que pertenezca al cliente (un cliente solo ve lo suyo). */
    public Cuenta buscarDeCliente(int cuentaId, int clienteId) {
        Cuenta cuenta = buscar(cuentaId);
        verificarPropietario(cuenta, clienteId);
        return cuenta;
    }

    /**
     * El cliente fija su propio limite por operacion (idea tomada del "Modo Calle" de Interbank):
     * si le roban el celular, el ladron no puede mover mas de ese monto por operacion.
     */
    public void cambiarLimite(int clienteId, int cuentaId, BigDecimal nuevoLimite) {
        Cuenta cuenta = buscarDeCliente(cuentaId, clienteId);
        if (nuevoLimite == null || nuevoLimite.signum() <= 0 || nuevoLimite.scale() > 2
                || nuevoLimite.compareTo(Cuenta.LIMITE_MAXIMO_CONFIGURABLE) > 0) {
            throw new NegocioException("El limite debe estar entre S/ 1 y S/ "
                    + Cuenta.LIMITE_MAXIMO_CONFIGURABLE.toPlainString());
        }
        cuentaDAO.actualizarLimite(cuenta.getId(), nuevoLimite);
    }

    public List<Transaccion> historial(int cuentaId) {
        return transaccionDAO.listarPorCuenta(cuentaId);
    }

    public Transaccion buscarTransaccion(int transaccionId) {
        return transaccionDAO.buscarPorId(transaccionId)
                .orElseThrow(() -> new NegocioException("El comprobante no existe"));
    }

    // ------------------------------------------------------------------ operaciones de caja (cajero)

    /**
     * Movimiento de ventanilla. Lo llaman los comandos del patron COMMAND (ComandoDeposito y ComandoRetiro),
     * tanto al ejecutar como al deshacer (extorno). El tipo indica si el dinero entra o sale.
     */
    public Transaccion moverEnCaja(int cuentaId, TipoTransaccion tipo, BigDecimal monto, String descripcion) {
        return TransaccionJdbc.ejecutar(() -> {
            Cuenta cuenta = buscar(cuentaId);
            if (esRecaudadora(cuenta)) {
                throw new NegocioException("La cuenta de recaudacion es interna: no admite depositos ni retiros en caja");
            }
            // GRASP Experto: la propia cuenta valida y calcula el nuevo saldo
            if (tipo.esIngreso()) {
                cuenta.depositar(monto);
            } else {
                cuenta.retirar(monto);
            }
            cuentaDAO.actualizarSaldo(cuenta.getId(), cuenta.getSaldo());
            return registrarMovimiento(cuenta, tipo, monto, descripcion);
        });
    }

    /** Abono del credito aprobado en la cuenta del cliente (lo llama CreditoService dentro de su transaccion). */
    public Transaccion desembolsar(int cuentaId, int clienteId, BigDecimal monto, String descripcion) {
        return TransaccionJdbc.ejecutar(() -> {
            Cuenta cuenta = buscarDeCliente(cuentaId, clienteId);
            cuenta.depositar(monto);
            cuentaDAO.actualizarSaldo(cuenta.getId(), cuenta.getSaldo());
            return registrarMovimiento(cuenta, TipoTransaccion.DESEMBOLSO_CREDITO, monto, descripcion);
        });
    }

    // ------------------------------------------------------------------ operaciones del cliente (canal digital)

    /** Desde este monto se pide confirmar con la clave (idea tomada del Token Digital del BCP). */
    public BigDecimal umbralConfirmacion() {
        return parametroService.obtenerDecimal(ParametroService.UMBRAL_MONTO_ALTO, new BigDecimal("3000"));
    }

    public Transaccion transferir(int clienteId, int cuentaOrigenId, String numeroDestino,
                                  BigDecimal monto, String descripcion, String username, String claveConfirmacion) {
        if (Validaciones.vacio(numeroDestino)) {
            throw new NegocioException("Indica el numero de la cuenta destino");
        }
        verificarConfirmacion(username, monto, claveConfirmacion);
        return TransaccionJdbc.ejecutar(() -> {
            Cuenta origen = buscarDeCliente(cuentaOrigenId, clienteId);
            Cuenta destino = cuentaDAO.buscarPorNumero(numeroDestino.trim())
                    .orElseThrow(() -> new NegocioException("La cuenta destino no existe"));
            if (esRecaudadora(destino)) {
                throw new NegocioException("Esa es una cuenta interna del banco; para pagar un servicio usa Pago de servicios");
            }
            if (origen.getId() == destino.getId()) {
                throw new NegocioException("La cuenta destino debe ser distinta de la cuenta origen");
            }
            // GRASP Experto: cada cuenta valida y calcula su propio saldo
            origen.validarLimiteOperacion(monto);
            origen.retirar(monto);
            destino.depositar(monto);
            cuentaDAO.actualizarSaldo(origen.getId(), origen.getSaldo());
            cuentaDAO.actualizarSaldo(destino.getId(), destino.getSaldo());

            String nota = Validaciones.vacio(descripcion) ? "" : " - " + descripcion.trim();
            Transaccion salida = registrarMovimiento(origen, TipoTransaccion.TRANSFERENCIA_ENVIADA, monto,
                    "Transferencia a " + destino.getNumero() + " (" + destino.getClienteNombre() + ")" + nota);
            registrarMovimiento(destino, TipoTransaccion.TRANSFERENCIA_RECIBIDA, monto,
                    "Transferencia de " + origen.getNumero() + " (" + origen.getClienteNombre() + ")" + nota);
            // PATRON OBSERVER: el cliente que recibe el dinero tiene que enterarse
            eventos.publicar(new EventoBancario(EventoBancario.Tipo.TRANSFERENCIA_RECIBIDA)
                    .cliente(destino.getClienteId()).cuenta(destino.getId(), destino.getNumero()).monto(monto)
                    .detalle("Te la envio " + origen.getClienteNombre() + "."));
            return salida;
        });
    }

    /**
     * Pago de servicios. El cobro lo hace la empresa externa a traves del ADAPTER: si la empresa
     * rechaza el pago se lanza una excepcion y la transaccion hace rollback (el saldo no cambia).
     */
    public Transaccion pagarServicio(int clienteId, int cuentaId, String servicio, String referencia, BigDecimal monto,
                                     String username, String claveConfirmacion) {
        if (Validaciones.vacio(servicio) || Validaciones.vacio(referencia)) {
            throw new NegocioException("Indica el servicio y el codigo de suministro");
        }
        verificarConfirmacion(username, monto, claveConfirmacion);
        return TransaccionJdbc.ejecutar(() -> {
            Cuenta cuenta = buscarDeCliente(cuentaId, clienteId);
            cuenta.validarLimiteOperacion(monto);
            cuenta.retirar(monto);
            String codigoOperacion = pasarelaPagos.pagar(servicio, referencia.trim(), monto);
            cuentaDAO.actualizarSaldo(cuenta.getId(), cuenta.getSaldo());
            String detalle = "Pago de " + servicio + " (suministro " + referencia.trim() + ", operacion "
                    + codigoOperacion + ")";
            Transaccion pago = registrarMovimiento(cuenta, TipoTransaccion.PAGO_SERVICIO, monto, detalle);

            // El dinero entra a la cuenta NEXO Recaudacion en la misma transaccion (si algo falla, rollback de todo)
            Cuenta recaudadora = cuentaDAO.buscarPorNumero(NUMERO_RECAUDADORA)
                    .orElseThrow(() -> new NegocioException("Falta la cuenta interna de recaudacion; reinicia el sistema"));
            recaudadora.depositar(monto);
            cuentaDAO.actualizarSaldo(recaudadora.getId(), recaudadora.getSaldo());
            Transaccion ingreso = new Transaccion();
            ingreso.setCuentaId(recaudadora.getId());
            ingreso.setTipo(TipoTransaccion.RECAUDACION_SERVICIO);
            ingreso.setMonto(monto);
            ingreso.setSaldoResultante(recaudadora.getSaldo());
            ingreso.setDescripcion(recortar(servicio + " - cuenta " + cuenta.getNumero() + " - operacion " + codigoOperacion));
            transaccionDAO.insertar(ingreso);
            return pago;
        });
    }

    // ------------------------------------------------------------------ interno

    private static String recortar(String descripcion) {
        String texto = Validaciones.vacio(descripcion) ? null : descripcion.trim();
        if (texto != null && texto.length() > LARGO_MAXIMO_DESCRIPCION) {
            texto = texto.substring(0, LARGO_MAXIMO_DESCRIPCION);
        }
        return texto;
    }

    private Transaccion registrarMovimiento(Cuenta cuenta, TipoTransaccion tipo, BigDecimal monto, String descripcion) {
        String texto = recortar(descripcion);
        Transaccion t = new Transaccion();
        t.setCuentaId(cuenta.getId());
        t.setTipo(tipo);
        t.setMonto(monto);
        t.setSaldoResultante(cuenta.getSaldo());
        t.setDescripcion(texto);
        transaccionDAO.insertar(t);
        // PATRON OBSERVER: el monitor de riesgo revisa cada movimiento (monto alto, operaciones seguidas)
        eventos.publicar(new EventoBancario(EventoBancario.Tipo.MOVIMIENTO_REGISTRADO)
                .cliente(cuenta.getClienteId()).cuenta(cuenta.getId(), cuenta.getNumero()).monto(monto)
                .detalle(tipo.name()));
        return t;
    }

    /** Segunda capa para montos altos: el cliente vuelve a escribir su clave antes de mover el dinero. */
    private void verificarConfirmacion(String username, BigDecimal monto, String clave) {
        if (monto != null && monto.compareTo(umbralConfirmacion()) >= 0) {
            if (Validaciones.vacio(clave)) {
                throw new NegocioException("Por seguridad, las operaciones desde S/ " + umbralConfirmacion().toPlainString()
                        + " se confirman con tu clave");
            }
            if (!usuarioService.verificarClave(username, clave)) {
                throw new NegocioException("La clave de confirmacion no es correcta. La operacion no se realizo");
            }
        }
    }

    private void verificarPropietario(Cuenta cuenta, int clienteId) {
        if (cuenta.getClienteId() != clienteId) {
            throw new NegocioException("La cuenta no pertenece al cliente");
        }
    }
}
