package com.banco.patterns.memento;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PATRON DE COMPORTAMIENTO: MEMENTO (la "foto").
 * Guarda una copia de todos los parametros en un momento dado. Es INMUTABLE: una vez creada,
 * nadie puede cambiar sus valores. Solo el Originador (ConfiguracionParametros) usa su estado
 * completo para restaurarse; el resto del sistema solo ve una vista de lectura.
 */
public final class ParametrosMemento {

    private final int id;
    private final Map<String, String> estado;
    private final String usuario;
    private final String descripcion;
    private final LocalDateTime fecha;

    ParametrosMemento(int id, Map<String, String> estado, String usuario, String descripcion, LocalDateTime fecha) {
        this.id = id;
        this.estado = Collections.unmodifiableMap(new LinkedHashMap<>(estado));
        this.usuario = usuario;
        this.descripcion = descripcion;
        this.fecha = fecha;
    }

    /** Solo el Originador y el Cuidador (mismo paquete) acceden al estado completo. */
    Map<String, String> getEstado() {
        return estado;
    }

    /** Copia con el id que le asigno la base de datos al guardarla. */
    ParametrosMemento conId(int nuevoId) {
        return new ParametrosMemento(nuevoId, estado, usuario, descripcion, fecha);
    }

    /** Convierte la foto a texto ("CLAVE=valor" por linea) para guardarla en la base de datos. */
    String comoTexto() {
        StringBuilder sb = new StringBuilder();
        estado.forEach((clave, valor) -> sb.append(clave).append('=').append(valor).append('\n'));
        return sb.toString();
    }

    static Map<String, String> desdeTexto(String texto) {
        Map<String, String> valores = new LinkedHashMap<>();
        for (String linea : texto.split("\n")) {
            int igual = linea.indexOf('=');
            if (igual > 0) {
                valores.put(linea.substring(0, igual).trim(), linea.substring(igual + 1).trim());
            }
        }
        return valores;
    }

    public int getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getDescripcion() { return descripcion; }
    public LocalDateTime getFecha() { return fecha; }

    /** Vista de solo lectura para mostrar en pantalla que valores tenia esta version. */
    public Map<String, String> getVista() {
        return estado;
    }
}
