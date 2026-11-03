package com.banco.dao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Guarda las versiones de la configuracion (patron Memento) como texto en parametro_historial. */
public interface ParametroHistorialDAO {

    /** Una fila tal como esta en la base de datos. */
    record Registro(int id, String valores, String usuario, String descripcion, LocalDateTime fecha) { }

    int insertar(String valores, String usuario, String descripcion);

    List<Registro> listar(int limite);

    Optional<Registro> buscarPorId(int id);
}
