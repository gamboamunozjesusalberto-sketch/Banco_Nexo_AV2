package com.banco.config;

import com.banco.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * SEGURIDAD: si el usuario entro con una clave temporal, no puede abrir ninguna pagina
 * hasta que la cambie (se le redirige siempre a /perfil/clave).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                HttpSession sesion = request.getSession(false);
                if (sesion != null && Boolean.TRUE.equals(sesion.getAttribute(AuthController.DEBE_CAMBIAR_CLAVE))) {
                    response.sendRedirect(request.getContextPath() + "/perfil/clave");
                    return false;
                }
                return true;
            }
        }).excludePathPatterns("/perfil/clave", "/login", "/logout", "/error", "/acceso-denegado",
                "/css/**", "/img/**", "/js/**", "/webjars/**", "/favicon.ico");
    }
}
