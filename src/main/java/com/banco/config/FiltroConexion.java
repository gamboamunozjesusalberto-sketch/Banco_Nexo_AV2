package com.banco.config;

import com.banco.util.ConexionBD;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Al terminar cada peticion web, devuelve al pool la conexion que uso (aunque haya habido un error).
 * Va primero que todos los filtros, incluido Spring Security, porque el login tambien consulta la base.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FiltroConexion extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            ConexionBD.liberarSiExiste();
        }
    }
}
