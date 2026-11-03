package com.banco.service;

import com.banco.dao.ParametroDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Cuenta;
import com.banco.model.Parametro;
import com.banco.patterns.memento.ConfiguracionParametros;
import com.banco.patterns.memento.HistorialParametros;
import com.banco.patterns.memento.ParametrosMemento;
import com.banco.util.NegocioException;
import com.banco.util.TransaccionJdbc;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lee y modifica los parametros de configuracion (los numeros que antes estarian "magicos" en el codigo).
 * PATRON MEMENTO: antes de cada cambio se guarda una foto de toda la configuracion, y el administrador
 * puede volver a cualquier version anterior (idea tomada de Nubank: limites configurables y reversibles).
 */
@Service
public class ParametroService {

    public static final String LIMITE_OPERACION_DEFECTO = "LIMITE_OPERACION_DEFECTO";
    public static final String MONTO_MAXIMO_CREDITO = "MONTO_MAXIMO_CREDITO";
    public static final String TASA_INTERES_ANUAL = "TASA_INTERES_ANUAL";
    public static final String UMBRAL_MONTO_ALTO = "UMBRAL_MONTO_ALTO";
    public static final String MAX_INTENTOS_FALLIDOS = "MAX_INTENTOS_FALLIDOS";
    public static final String MAX_OPERACIONES_VENTANA = "MAX_OPERACIONES_VENTANA";
    public static final String VENTANA_MINUTOS = "VENTANA_MINUTOS";
    public static final String MINUTOS_BLOQUEO = "MINUTOS_BLOQUEO";

    /** Parametros que solo aceptan numeros enteros. */
    private static final Set<String> ENTEROS = Set.of(MAX_INTENTOS_FALLIDOS, MAX_OPERACIONES_VENTANA,
            VENTANA_MINUTOS, MINUTOS_BLOQUEO);
    private static final BigDecimal TASA_MAXIMA = new BigDecimal("100");

    private final ParametroDAO parametroDAO = DAOFactory.getParametroDAO();
    private final HistorialParametros historial;

    public ParametroService(HistorialParametros historial) {
        this.historial = historial;
    }

    public BigDecimal obtenerDecimal(String clave, BigDecimal porDefecto) {
        return parametroDAO.obtenerValor(clave).map(valor -> {
            try {
                return new BigDecimal(valor);
            } catch (NumberFormatException e) {
                return porDefecto;
            }
        }).orElse(porDefecto);
    }

    public int obtenerEntero(String clave, int porDefecto) {
        return obtenerDecimal(clave, BigDecimal.valueOf(porDefecto)).intValue();
    }

    public List<Parametro> listar() {
        return parametroDAO.listar();
    }

    /** Cambia un parametro. Antes guarda una foto (Memento) de como estaba toda la configuracion. */
    public void actualizar(String clave, String valor, String usuario) {
        Parametro actual = parametroDAO.listar().stream().filter(p -> p.getClave().equals(clave)).findFirst()
                .orElseThrow(() -> new NegocioException("El parametro no existe"));
        String nuevo = validar(clave, valor);
        if (nuevo.equals(actual.getValor())) {
            throw new NegocioException("El parametro ya tiene ese valor");
        }
        TransaccionJdbc.ejecutar(() -> {
            ConfiguracionParametros originador = new ConfiguracionParametros(parametroDAO.listar());
            historial.guardar(originador.crearMemento(usuario,
                    "Antes de cambiar " + clave + " de " + actual.getValor() + " a " + nuevo));
            parametroDAO.actualizar(clave, nuevo);
            return null;
        });
    }

    /** Lista de versiones guardadas, de la mas reciente a la mas antigua. */
    public List<ParametrosMemento> versiones() {
        return historial.listar();
    }

    /**
     * Vuelve a una version anterior. Primero guarda la configuracion actual, asi la restauracion
     * tambien se puede deshacer. Todo va en una transaccion: si algo falla, no cambia ningun valor.
     */
    public void restaurar(int versionId, String usuario) {
        ParametrosMemento version = historial.obtener(versionId);
        TransaccionJdbc.ejecutar(() -> {
            ConfiguracionParametros originador = new ConfiguracionParametros(parametroDAO.listar());
            historial.guardar(originador.crearMemento(usuario, "Antes de restaurar la version #" + versionId));
            originador.restaurar(version);
            for (Map.Entry<String, String> p : originador.getValores().entrySet()) {
                parametroDAO.actualizar(p.getKey(), p.getValue());
            }
            return null;
        });
    }

    private String validar(String clave, String valor) {
        BigDecimal numero;
        try {
            numero = new BigDecimal(valor == null ? "" : valor.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException("El valor debe ser un numero");
        }
        if (numero.signum() <= 0 || numero.scale() > 2) {
            throw new NegocioException("El valor debe ser mayor a 0 y con maximo 2 decimales");
        }
        if (ENTEROS.contains(clave)) {
            if (numero.stripTrailingZeros().scale() > 0) {
                throw new NegocioException("Este parametro debe ser un numero entero");
            }
            return numero.toBigInteger().toString();
        }
        if (LIMITE_OPERACION_DEFECTO.equals(clave) && numero.compareTo(Cuenta.LIMITE_MAXIMO_CONFIGURABLE) > 0) {
            throw new NegocioException("El limite por defecto no puede superar S/ " + Cuenta.LIMITE_MAXIMO_CONFIGURABLE);
        }
        if (TASA_INTERES_ANUAL.equals(clave) && numero.compareTo(TASA_MAXIMA) > 0) {
            throw new NegocioException("La tasa anual no puede superar el 100 %");
        }
        return numero.toPlainString();
    }
}
