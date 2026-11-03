package com.banco.model;

import com.banco.patterns.state.EstadoCredito;
import com.banco.patterns.state.EstadoPendiente;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Solicitud de credito de un cliente.
 *
 * PATRON STATE (Contexto): guarda una referencia a su estado actual (EstadoCredito) y le delega
 * evaluar, aprobar, rechazar y desembolsar. No tiene if segun el estado.
 *
 * GRASP EXPERTO: la solicitud conoce el monto, el plazo y la tasa, por eso ella misma calcula
 * su cuota mensual y su cronograma de pagos.
 */
public class SolicitudCredito {

    private int id;
    private int clienteId;
    private String clienteNombre;
    private BigDecimal monto;
    private int plazoMeses;
    private String motivo;
    private EstadoCredito estado = new EstadoPendiente();
    private String observaciones;
    private Integer asesorId;
    private Integer cuentaDestinoId;
    private String cuentaDestinoNumero;
    private BigDecimal tasaAnual;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaResolucion;

    // ------------------------------------------------ PATRON STATE: el contexto delega en su estado

    public void evaluar() { estado.evaluar(this); }

    public void aprobar() { estado.aprobar(this); }

    public void rechazar() { estado.rechazar(this); }

    public void desembolsar() { estado.desembolsar(this); }

    /** Lo llaman los propios estados para hacer la transicion. */
    public void cambiarEstado(EstadoCredito nuevo) {
        this.estado = nuevo;
    }

    public boolean isAdmiteDecision() { return estado.admiteDecision(); }

    public boolean isPendiente() { return estado.getValor() == EstadoSolicitud.PENDIENTE; }

    public boolean isFinalizada() { return estado.esFinal(); }

    // ------------------------------------------------ GRASP EXPERTO: calculos del credito

    /** Cuota fija mensual (sistema frances): C = P * i / (1 - (1 + i)^-n). */
    public BigDecimal getCuotaMensual() {
        if (monto == null || tasaAnual == null || plazoMeses <= 0) {
            return null;
        }
        BigDecimal i = tasaMensual();
        if (i.signum() == 0) {
            return monto.divide(BigDecimal.valueOf(plazoMeses), 2, RoundingMode.HALF_UP);
        }
        MathContext mc = MathContext.DECIMAL64;
        BigDecimal factor = BigDecimal.ONE.add(i).pow(-plazoMeses, mc);
        return monto.multiply(i, mc).divide(BigDecimal.ONE.subtract(factor), mc).setScale(2, RoundingMode.HALF_UP);
    }

    /** Total que el cliente pagara al final (todas las cuotas). */
    public BigDecimal getTotalAPagar() {
        BigDecimal cuota = getCuotaMensual();
        return cuota == null ? null : cuota.multiply(BigDecimal.valueOf(plazoMeses));
    }

    /** Cronograma de pagos: una fila por mes, empezando un mes despues del desembolso. */
    public List<CuotaCredito> getCronograma() {
        List<CuotaCredito> filas = new ArrayList<>();
        BigDecimal cuota = getCuotaMensual();
        if (cuota == null) {
            return filas;
        }
        BigDecimal i = tasaMensual();
        BigDecimal saldo = monto;
        LocalDate inicio = (fechaResolucion != null ? fechaResolucion.toLocalDate() : LocalDate.now());
        for (int n = 1; n <= plazoMeses; n++) {
            BigDecimal interes = saldo.multiply(i).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amortizacion = cuota.subtract(interes);
            BigDecimal cuotaFila = cuota;
            if (n == plazoMeses) { // la ultima cuota ajusta el redondeo
                amortizacion = saldo;
                cuotaFila = saldo.add(interes);
            }
            saldo = saldo.subtract(amortizacion);
            filas.add(new CuotaCredito(n, inicio.plusMonths(n), cuotaFila, interes, amortizacion,
                    saldo.max(BigDecimal.ZERO)));
        }
        return filas;
    }

    private BigDecimal tasaMensual() {
        return tasaAnual.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------ getters y setters

    public EstadoSolicitud getEstado() { return estado.getValor(); }
    public void setEstado(EstadoSolicitud valor) { this.estado = valor.comoEstado(); }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getClienteId() { return clienteId; }
    public void setClienteId(int clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public int getPlazoMeses() { return plazoMeses; }
    public void setPlazoMeses(int plazoMeses) { this.plazoMeses = plazoMeses; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Integer getAsesorId() { return asesorId; }
    public void setAsesorId(Integer asesorId) { this.asesorId = asesorId; }
    public Integer getCuentaDestinoId() { return cuentaDestinoId; }
    public void setCuentaDestinoId(Integer cuentaDestinoId) { this.cuentaDestinoId = cuentaDestinoId; }
    public String getCuentaDestinoNumero() { return cuentaDestinoNumero; }
    public void setCuentaDestinoNumero(String cuentaDestinoNumero) { this.cuentaDestinoNumero = cuentaDestinoNumero; }
    public BigDecimal getTasaAnual() { return tasaAnual; }
    public void setTasaAnual(BigDecimal tasaAnual) { this.tasaAnual = tasaAnual; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(LocalDateTime fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
}
