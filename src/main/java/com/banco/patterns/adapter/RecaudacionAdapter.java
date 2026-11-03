package com.banco.patterns.adapter;

import com.banco.util.NegocioException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PATRON ESTRUCTURAL: ADAPTER (adaptador).
 * Implementa la interfaz que espera el banco (PasarelaPagoServicios) y por dentro usa el sistema
 * externo (SistemaRecaudacionExterno), traduciendo entre los dos:
 *  - nombre del servicio  -> codigo numerico de empresa
 *  - soles (BigDecimal)   -> centimos (long)
 *  - codigo de estado     -> excepcion de negocio con mensaje claro
 * Si manana se cambia de empresa recaudadora, solo se crea otro adaptador; CuentaService no cambia.
 */
@Component
public class RecaudacionAdapter implements PasarelaPagoServicios {

    private static final Map<String, Integer> CODIGO_EMPRESA = Map.of(
            "Luz", 101,
            "Agua", 102,
            "Internet", 103,
            "Telefonia movil", 104,
            "Gas", 105);

    private final SistemaRecaudacionExterno sistemaExterno = new SistemaRecaudacionExterno();

    @Override
    public String pagar(String servicio, String referencia, BigDecimal monto) {
        Integer codigoEmpresa = CODIGO_EMPRESA.get(servicio);
        if (codigoEmpresa == null) {
            throw new NegocioException("El servicio " + servicio + " no esta disponible");
        }
        long centimos = monto.movePointRight(2).longValueExact();

        int estado = sistemaExterno.procesarCobro(codigoEmpresa, referencia, centimos);
        return switch (estado) {
            case SistemaRecaudacionExterno.COBRO_OK -> sistemaExterno.obtenerUltimoCodigoOperacion();
            case SistemaRecaudacionExterno.EMPRESA_NO_EXISTE ->
                    throw new NegocioException("La empresa del servicio no esta afiliada");
            case SistemaRecaudacionExterno.SUMINISTRO_INVALIDO ->
                    throw new NegocioException("La empresa rechazo el pago: revisa el codigo de suministro "
                            + "(6 a 12 digitos) y que el monto no supere S/ 2,000.00");
            default -> throw new NegocioException("La empresa no pudo procesar el pago (codigo " + estado + ")");
        };
    }
}
