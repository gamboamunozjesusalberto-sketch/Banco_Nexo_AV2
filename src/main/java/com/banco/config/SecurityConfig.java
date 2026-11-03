package com.banco.config;

import com.banco.service.AuditoriaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import com.banco.controller.AuthController;
import com.banco.service.UsuarioService;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SEGURIDAD con Spring Security:
 *  - Login con formulario propio (/login) y cierre de sesion (/logout, por POST).
 *  - Contrasenas cifradas con BCrypt.
 *  - Control de acceso por rol: cada actor solo entra a sus propias paginas.
 *  - Proteccion CSRF activa (los formularios Thymeleaf incluyen el token automaticamente).
 *  - Cada ingreso, salida e intento fallido queda en la bitacora de accesos.
 *  - (Avance 2) Bloqueo temporal tras varios intentos fallidos y cambio obligatorio de clave temporal.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuditoriaService auditoria,
                                           UsuarioService usuarioService) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/registro", "/acceso-denegado", "/error", "/webjars/**",
                        "/css/**", "/img/**", "/js/**", "/favicon.ico").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/cliente/**").hasRole("CLIENTE")
                .requestMatchers("/asesor/**").hasRole("ASESOR")
                .requestMatchers("/cajero/**").hasRole("CAJERO")
                .requestMatchers("/auditor/**").hasRole("AUDITOR")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler((request, response, authentication) -> {
                    auditoria.registrarAcceso(authentication.getName(), "LOGIN_OK", "Ingreso al sistema");
                    if (usuarioService.debeCambiarClave(authentication.getName())) {
                        // clave temporal: no puede usar el sistema hasta cambiarla
                        request.getSession().setAttribute(AuthController.DEBE_CAMBIAR_CLAVE, Boolean.TRUE);
                        response.sendRedirect(request.getContextPath() + "/perfil/clave");
                        return;
                    }
                    response.sendRedirect(request.getContextPath() + "/");
                })
                .failureHandler((request, response, exception) -> {
                    String usuario = request.getParameter("username");
                    String nombre = usuario == null ? "(vacio)" : (usuario.length() > 30 ? usuario.substring(0, 30) : usuario);
                    if (exception instanceof LockedException) {
                        auditoria.registrarAcceso(nombre, "LOGIN_BLOQUEADO", "Intento con usuario bloqueado");
                        response.sendRedirect(request.getContextPath() + "/login?bloqueado");
                        return;
                    }
                    String motivo = exception instanceof DisabledException ? "Usuario desactivado" : "Credenciales incorrectas";
                    auditoria.registrarAcceso(nombre, "LOGIN_FALLO", motivo);
                    response.sendRedirect(request.getContextPath() + "/login?error");
                })
                .permitAll())
            .logout(logout -> logout
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        auditoria.registrarAcceso(authentication.getName(), "LOGOUT", "Salida del sistema");
                    }
                    response.sendRedirect(request.getContextPath() + "/login?logout");
                }))
            .exceptionHandling(ex -> ex.accessDeniedPage("/acceso-denegado"));
        return http.build();
    }
}
