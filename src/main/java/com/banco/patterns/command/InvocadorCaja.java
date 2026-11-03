package com.banco.patterns.command;

import com.banco.dao.OperacionCajaDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.OperacionCaja;
import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;
import com.banco.service.CuentaService;
import com.banco.util.NegocioException;
import com.banco.util.TransaccionJdbc;
import com.banco.util.Validaciones;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * PATRON COMMAND (Invocador).
 * Recibe cualquier ComandoCaja, lo ejecuta y deja constancia en el historial (operacion_caja):
 * quien lo hizo, cuando, sobre que cuenta y por cuanto. Con ese historial el administrador
 * audita las operaciones y puede extornar (deshacer) una que se hizo por error.
 * No sabe si el comando es un deposito o un retiro: solo conoce la interfaz.
 */
@Component
public class InvocadorCaja {

    private static final int OPERACIONES_VISIBLES = 300;
    private static final int LARGO_MAXIMO_MOTIVO = 150;

    private final OperacionCajaDAO historialDAO = DAOFactory.getOperacionCajaDAO();
    private final CuentaService receptor;

    public InvocadorCaja(CuentaService receptor) {
        this.receptor = receptor;
    }

    /** Fabrica los comandos (asi el controlador no hace new de clases concretas). */
    public ComandoCaja crearComando(TipoTransaccion tipo, int cuentaId, BigDecimal monto, String descripcion) {
        return switch (tipo) {
            case DEPOSITO -> new ComandoDeposito(receptor, cuentaId, monto, descripcion);
            case RETIRO -> new ComandoRetiro(receptor, cuentaId, monto, descripcion);
            default -> throw new NegocioException("La ventanilla solo registra depositos y retiros");
        };
    }

    /** Ejecuta el comando y lo guarda en el historial, todo en una sola transaccion. */
    public Transaccion ejecutar(ComandoCaja comando, String cajero) {
        return TransaccionJdbc.ejecutar(() -> {
            Transaccion t = comando.ejecutar();
            OperacionCaja registro = new OperacionCaja();
            registro.setComando(comando.getTipo());
            registro.setCuentaId(comando.getCuentaId());
            registro.setMonto(comando.getMonto());
            registro.setCajero(cajero);
            registro.setTransaccionId(t.getId());
            historialDAO.insertar(registro);
            return t;
        });
    }

    /** Deshace una operacion del historial: rearma su comando y llama a deshacer(). */
    public Transaccion extornar(int operacionId, String usuario, String motivo) {
        if (Validaciones.vacio(motivo) || motivo.trim().length() > LARGO_MAXIMO_MOTIVO) {
            throw new NegocioException("Indica el motivo del extorno (maximo " + LARGO_MAXIMO_MOTIVO + " caracteres)");
        }
        return TransaccionJdbc.ejecutar(() -> {
            OperacionCaja registro = historialDAO.buscarPorId(operacionId)
                    .orElseThrow(() -> new NegocioException("La operacion no existe"));
            if (!registro.isExtornable()) {
                throw new NegocioException("La operacion ya fue extornada");
            }
            ComandoCaja comando = crearComando(registro.getComando(), registro.getCuentaId(), registro.getMonto(), null);
            Transaccion extorno = comando.deshacer(motivo.trim());
            historialDAO.marcarExtornada(operacionId, extorno.getId(), usuario, motivo.trim());
            return extorno;
        });
    }

    public List<OperacionCaja> historial() {
        return historialDAO.listar(OPERACIONES_VISIBLES);
    }
}
