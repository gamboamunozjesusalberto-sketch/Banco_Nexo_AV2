package com.banco.controller;

import com.banco.util.DatabaseException;
import com.banco.util.NegocioException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Atrapa los errores que no manejo cada controlador. Los errores de negocio se muestran tal cual;
 * los tecnicos (base de datos) se muestran con un mensaje generico para no revelar detalles internos.
 */
@ControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(NegocioException.class)
    public String negocio(NegocioException e, Model model) {
        model.addAttribute("mensaje", e.getMessage());
        return "error-general";
    }

    @ExceptionHandler(DatabaseException.class)
    public String baseDeDatos(DatabaseException e, Model model) {
        System.err.println("Error de base de datos: " + e.getMessage());
        model.addAttribute("mensaje", "Ocurrio un problema al acceder a los datos. Intenta nuevamente.");
        return "error-general";
    }
}
