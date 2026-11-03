package com.banco.config;

import com.banco.dao.UsuarioDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Usuario;
import com.banco.service.UsuarioService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Puente entre Spring Security y nuestra base de datos: cuando alguien intenta ingresar, Spring
 * Security le pide a esta clase los datos del usuario, y esta clase los lee con JDBC (UsuarioDAO).
 * Spring Security compara luego la contrasena con el hash BCrypt.
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioDAO usuarioDAO = DAOFactory.getUsuarioDAO();
    private final UsuarioService usuarioService;

    public UsuarioDetailsService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = usuarioDAO.buscarPorUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(u.getUsername())
                .password(u.getPasswordHash())
                .roles(u.getRol().name())
                .disabled(!u.isActivo())
                // bloqueo temporal por intentos fallidos: ni con la clave correcta puede entrar
                .accountLocked(usuarioService.estaBloqueado(u.getUsername()))
                .build();
    }
}
