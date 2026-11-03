package com.banco.patterns.memento;

import com.banco.model.Parametro;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PATRON MEMENTO (Originador).
 * Representa la configuracion actual del banco (todos los parametros juntos).
 * Sabe crear una foto de si misma (crearMemento) y volver a una foto anterior (restaurar).
 */
public class ConfiguracionParametros {

    private final Map<String, String> valores = new LinkedHashMap<>();

    public ConfiguracionParametros(List<Parametro> actuales) {
        actuales.forEach(p -> valores.put(p.getClave(), p.getValor()));
    }

    public ParametrosMemento crearMemento(String usuario, String descripcion) {
        return new ParametrosMemento(0, valores, usuario, descripcion, LocalDateTime.now());
    }

    /** Vuelve a los valores de la foto. Solo se restauran los parametros que siguen existiendo. */
    public void restaurar(ParametrosMemento memento) {
        memento.getEstado().forEach((clave, valor) -> {
            if (valores.containsKey(clave)) {
                valores.put(clave, valor);
            }
        });
    }

    public Map<String, String> getValores() {
        return Collections.unmodifiableMap(valores);
    }
}
