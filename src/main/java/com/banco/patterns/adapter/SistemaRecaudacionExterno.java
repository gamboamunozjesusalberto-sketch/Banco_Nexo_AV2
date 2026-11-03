package com.banco.patterns.adapter;

/**
 * PATRON ESTRUCTURAL: ADAPTER (clase adaptada o "Adaptee").
 * Simula el sistema de una empresa recaudadora externa (luz, agua, internet...). Su interfaz es
 * distinta a la del banco y NO se puede modificar (en la vida real seria una libreria o API de terceros):
 *  - identifica cada empresa con un codigo numerico, no con su nombre;
 *  - recibe el monto en CENTIMOS (long), no en soles;
 *  - responde con un codigo de estado numerico en lugar de lanzar excepciones.
 * Es una simulacion academica: no se conecta a ninguna empresa real.
 */
public class SistemaRecaudacionExterno {

    public static final int COBRO_OK = 0;
    public static final int EMPRESA_NO_EXISTE = 10;
    public static final int SUMINISTRO_INVALIDO = 20;

    private static final long MAXIMO_CENTIMOS = 200_000L;

    private long correlativo = 100_000L;
    private String ultimoCodigoOperacion;

    /** Registra el cobro y devuelve un codigo de estado (0 = aceptado). */
    public synchronized int procesarCobro(int codigoEmpresa, String numeroSuministro, long montoCentimos) {
        if (codigoEmpresa < 101 || codigoEmpresa > 105) {
            return EMPRESA_NO_EXISTE;
        }
        if (numeroSuministro == null || !numeroSuministro.matches("[0-9]{6,12}")) {
            return SUMINISTRO_INVALIDO;
        }
        if (montoCentimos <= 0 || montoCentimos > MAXIMO_CENTIMOS) {
            return SUMINISTRO_INVALIDO;
        }
        correlativo++;
        ultimoCodigoOperacion = "REC-" + codigoEmpresa + "-" + correlativo;
        return COBRO_OK;
    }

    /** Codigo de la ultima operacion aceptada. */
    public synchronized String obtenerUltimoCodigoOperacion() {
        return ultimoCodigoOperacion;
    }
}
