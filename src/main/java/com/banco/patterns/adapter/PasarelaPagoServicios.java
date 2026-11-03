package com.banco.patterns.adapter;

import java.math.BigDecimal;

/**
 * PATRON ESTRUCTURAL: ADAPTER (interfaz objetivo o "Target").
 * Es la interfaz que el banco espera usar para pagar servicios: habla en soles (BigDecimal)
 * y con nombres de servicio ("Luz", "Agua"...). CuentaService solo conoce esta interfaz.
 */
public interface PasarelaPagoServicios {

    /**
     * Envia el pago a la empresa del servicio.
     *
     * @return codigo de operacion que entrega la empresa (va en la descripcion del movimiento)
     * @throws com.banco.util.NegocioException si la empresa rechaza el pago
     */
    String pagar(String servicio, String referencia, BigDecimal monto);
}
