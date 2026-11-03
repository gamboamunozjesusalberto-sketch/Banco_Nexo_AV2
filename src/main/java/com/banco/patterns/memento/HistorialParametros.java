package com.banco.patterns.memento;

import com.banco.dao.ParametroHistorialDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.util.NegocioException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PATRON MEMENTO (Cuidador).
 * Guarda y entrega las fotos de la configuracion, pero NUNCA las modifica ni decide que hacer
 * con sus valores. Las conserva en la tabla parametro_historial para que no se pierdan al reiniciar.
 */
@Component
public class HistorialParametros {

    private static final int VERSIONES_VISIBLES = 30;

    private final ParametroHistorialDAO historialDAO = DAOFactory.getParametroHistorialDAO();

    public ParametrosMemento guardar(ParametrosMemento memento) {
        int id = historialDAO.insertar(memento.comoTexto(), memento.getUsuario(), memento.getDescripcion());
        return memento.conId(id);
    }

    public ParametrosMemento obtener(int id) {
        return historialDAO.buscarPorId(id)
                .map(this::aMemento)
                .orElseThrow(() -> new NegocioException("La version de parametros no existe"));
    }

    public List<ParametrosMemento> listar() {
        return historialDAO.listar(VERSIONES_VISIBLES).stream().map(this::aMemento).toList();
    }

    private ParametrosMemento aMemento(ParametroHistorialDAO.Registro r) {
        return new ParametrosMemento(r.id(), ParametrosMemento.desdeTexto(r.valores()), r.usuario(),
                r.descripcion(), r.fecha());
    }
}
