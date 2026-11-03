package com.banco.dao;

import com.banco.model.TipoTransaccion;
import com.banco.model.Transaccion;

import java.util.List;
import java.util.Optional;

public interface TransaccionDAO {
    void insertar(Transaccion transaccion);
    List<Transaccion> listarPorCuenta(int cuentaId);
    Optional<Transaccion> buscarPorId(int id);
    /** Ultimas 300 transacciones; si tipo es null, de todos los tipos. */
    List<Transaccion> listarTodas(TipoTransaccion tipo);

    /** Movimientos de una cuenta registrados en los ultimos minutos (para detectar operaciones seguidas). */
    int contarRecientesPorCuenta(int cuentaId, int minutos);

    /** Reporte global: por cada tipo de movimiento, cantidad y monto total. Cada fila: tipo, cantidad, total. */
    List<Object[]> resumenPorTipo();

    /** Lo cobrado en la cuenta NEXO Recaudacion, agrupado por servicio. Cada fila: servicio, cantidad, total. */
    List<Object[]> recaudadoPorServicio();

    /** Reporte global: monto movido por dia en los ultimos dias. Cada fila: fecha (texto dd/MM), cantidad, total. */
    List<Object[]> resumenPorDia(int dias);
}
