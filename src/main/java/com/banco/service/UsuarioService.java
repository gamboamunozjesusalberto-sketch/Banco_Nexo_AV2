package com.banco.service;

import com.banco.dao.BitacoraDAO;
import com.banco.dao.UsuarioDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Rol;
import com.banco.model.Usuario;
import com.banco.util.NegocioException;
import com.banco.util.TransaccionJdbc;
import com.banco.util.Validaciones;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

/**
 * Gestion de usuarios y roles. La contrasena se guarda siempre cifrada con BCrypt
 * (PasswordEncoder de Spring Security); nunca se guarda el texto original.
 */
@Service
public class UsuarioService {

    private static final String LETRAS_CLAVE = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final int LARGO_CLAVE_TEMPORAL = 10;

    private final UsuarioDAO usuarioDAO = DAOFactory.getUsuarioDAO();
    private final BitacoraDAO bitacoraDAO = DAOFactory.getBitacoraDAO();
    private final PasswordEncoder passwordEncoder;
    private final ParametroService parametroService;
    private final SecureRandom azar = new SecureRandom();

    public UsuarioService(PasswordEncoder passwordEncoder, ParametroService parametroService) {
        this.passwordEncoder = passwordEncoder;
        this.parametroService = parametroService;
    }

    public Usuario crear(String username, String password, String nombre, Rol rol, Integer clienteId) {
        return crear(username, password, nombre, rol, clienteId, false);
    }

    /**
     * @param temporal true cuando la clave la puso otra persona (admin o cajero): el usuario
     *                 tendra que cambiarla en su primer ingreso, asi nadie mas conoce su clave final.
     */
    public Usuario crear(String username, String password, String nombre, Rol rol, Integer clienteId, boolean temporal) {
        if (!Validaciones.usernameValido(username)) {
            throw new NegocioException("El usuario debe tener entre 3 y 30 caracteres (letras, numeros, punto, guion)");
        }
        if (!Validaciones.passwordSegura(password)) {
            throw new NegocioException("La clave debe tener al menos 8 caracteres, con letras y numeros");
        }
        if (Validaciones.vacio(nombre)) {
            throw new NegocioException("El nombre es obligatorio");
        }
        if (rol == null) {
            throw new NegocioException("Debes elegir un rol");
        }
        if (usuarioDAO.existeUsername(username)) {
            throw new NegocioException("Ya existe un usuario con ese nombre de usuario");
        }
        Usuario u = new Usuario();
        u.setUsername(username.trim());
        u.setPasswordHash(passwordEncoder.encode(password));
        u.setNombre(nombre.trim());
        u.setRol(rol);
        u.setClienteId(clienteId);
        u.setDebeCambiarClave(temporal);
        usuarioDAO.insertar(u);
        return u;
    }

    /** El administrador solo crea personal del banco; los clientes se registran por otra via. */
    public Usuario crearPersonal(String username, String password, String nombre, Rol rol) {
        return crearPersonal(username, password, nombre, rol, false);
    }

    public Usuario crearPersonal(String username, String password, String nombre, Rol rol, boolean temporal) {
        if (rol == Rol.CLIENTE) {
            throw new NegocioException("Los clientes se registran desde el modulo de clientes");
        }
        return crear(username, password, nombre, rol, null, temporal);
    }

    /** Lista de usuarios marcando cuales estan bloqueados por intentos fallidos. */
    public List<Usuario> listar() {
        List<Usuario> usuarios = usuarioDAO.listar();
        usuarios.forEach(u -> u.setBloqueado(estaBloqueado(u.getUsername())));
        return usuarios;
    }

    // ----------------------------------------------------------------- seguridad de claves

    /** Compara una clave escrita con el hash BCrypt guardado (para confirmar operaciones de monto alto). */
    public boolean verificarClave(String username, String clave) {
        return clave != null && usuarioDAO.buscarPorUsername(username)
                .map(u -> passwordEncoder.matches(clave, u.getPasswordHash()))
                .orElse(false);
    }

    /**
     * Bloqueo temporal: si el usuario llego a MAX_INTENTOS_FALLIDOS dentro de MINUTOS_BLOQUEO
     * (sin un ingreso correcto ni un desbloqueo en medio), no puede ingresar aunque escriba bien la clave.
     */
    public boolean estaBloqueado(String username) {
        int maximo = parametroService.obtenerEntero(ParametroService.MAX_INTENTOS_FALLIDOS, 3);
        int minutos = parametroService.obtenerEntero(ParametroService.MINUTOS_BLOQUEO, 15);
        return bitacoraDAO.contarFallosRecientes(username, minutos) >= maximo;
    }

    /** El administrador quita el bloqueo antes de que pase el tiempo (queda en la bitacora). */
    public void desbloquear(int id, String usernameActual) {
        Usuario objetivo = usuarioDAO.buscarPorId(id)
                .orElseThrow(() -> new NegocioException("El usuario no existe"));
        bitacoraDAO.registrar(objetivo.getUsername(), "DESBLOQUEO", "Desbloqueado por " + usernameActual);
    }

    /**
     * Restablece la clave de un usuario que la olvido. Nadie puede VER la clave anterior (BCrypt es de una
     * sola via): se genera una clave temporal que se muestra una sola vez y el usuario debe cambiarla
     * en su siguiente ingreso. El cajero solo restablece clientes; el administrador, solo personal.
     */
    public String restablecerClave(int usuarioId, String usernameActual, Rol rolActual) {
        Usuario objetivo = usuarioDAO.buscarPorId(usuarioId)
                .orElseThrow(() -> new NegocioException("El usuario no existe"));
        if (objetivo.getUsername().equals(usernameActual)) {
            throw new NegocioException("Para cambiar tu propia clave usa la opcion Cambiar mi clave");
        }
        boolean esCliente = objetivo.getRol() == Rol.CLIENTE;
        if (rolActual == Rol.CAJERO && !esCliente) {
            throw new NegocioException("El cajero solo puede restablecer claves de clientes");
        }
        if (rolActual == Rol.ADMINISTRADOR && esCliente) {
            throw new NegocioException("Las claves de clientes las restablece el cajero, verificando su DNI");
        }
        if (rolActual != Rol.CAJERO && rolActual != Rol.ADMINISTRADOR) {
            throw new NegocioException("No tienes permiso para restablecer claves");
        }
        String temporal = generarClaveTemporal();
        return TransaccionJdbc.ejecutar(() -> {
            usuarioDAO.cambiarClave(objetivo.getId(), passwordEncoder.encode(temporal), true);
            bitacoraDAO.registrar(objetivo.getUsername(), "CLAVE_RESTABLECIDA", "Clave temporal generada por " + usernameActual);
            // restablecer la clave tambien quita el bloqueo por intentos fallidos
            bitacoraDAO.registrar(objetivo.getUsername(), "DESBLOQUEO", "Al restablecer la clave");
            return temporal;
        });
    }

    /** Usuario de acceso de un cliente (el cajero lo busca por el cliente que atiende). */
    public Usuario usuarioDeCliente(int clienteId) {
        return usuarioDAO.buscarPorClienteId(clienteId)
                .orElseThrow(() -> new NegocioException("El cliente no tiene usuario de acceso"));
    }

    /** El propio usuario cambia su clave (obligatorio si tenia una clave temporal). */
    public void cambiarClave(String username, String actual, String nueva, String confirmacion) {
        Usuario u = buscarPorUsername(username);
        if (!passwordEncoder.matches(actual == null ? "" : actual, u.getPasswordHash())) {
            throw new NegocioException("La clave actual no es correcta");
        }
        if (!Validaciones.passwordSegura(nueva)) {
            throw new NegocioException("La nueva clave debe tener al menos 8 caracteres, con letras y numeros");
        }
        if (!nueva.equals(confirmacion)) {
            throw new NegocioException("La confirmacion no coincide con la nueva clave");
        }
        if (passwordEncoder.matches(nueva, u.getPasswordHash())) {
            throw new NegocioException("La nueva clave debe ser distinta de la actual");
        }
        TransaccionJdbc.ejecutar(() -> {
            usuarioDAO.cambiarClave(u.getId(), passwordEncoder.encode(nueva), false);
            bitacoraDAO.registrar(username, "CLAVE_CAMBIADA", "El usuario cambio su clave");
            return null;
        });
    }

    public boolean debeCambiarClave(String username) {
        return usuarioDAO.buscarPorUsername(username).map(Usuario::isDebeCambiarClave).orElse(false);
    }

    private String generarClaveTemporal() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < LARGO_CLAVE_TEMPORAL - 2; i++) {
            sb.append(LETRAS_CLAVE.charAt(azar.nextInt(LETRAS_CLAVE.length())));
        }
        // garantiza al menos una letra y un numero
        sb.append((char) ('a' + azar.nextInt(26))).append(azar.nextInt(10));
        return sb.toString();
    }

    public void cambiarEstado(int id, boolean activo, String usernameActual) {
        Usuario objetivo = usuarioDAO.buscarPorId(id)
                .orElseThrow(() -> new NegocioException("El usuario no existe"));
        if (objetivo.getUsername().equals(usernameActual)) {
            throw new NegocioException("No puedes desactivar tu propio usuario");
        }
        usuarioDAO.cambiarEstado(id, activo);
    }

    public void cambiarRol(int id, Rol nuevoRol, String usernameActual) {
        Usuario objetivo = usuarioDAO.buscarPorId(id)
                .orElseThrow(() -> new NegocioException("El usuario no existe"));
        if (objetivo.getUsername().equals(usernameActual)) {
            throw new NegocioException("No puedes cambiar tu propio rol");
        }
        if (objetivo.getRol() == Rol.CLIENTE || nuevoRol == Rol.CLIENTE) {
            throw new NegocioException("Los roles de clientes no se modifican desde aqui");
        }
        usuarioDAO.cambiarRol(id, nuevoRol);
    }

    public Usuario buscarPorUsername(String username) {
        return usuarioDAO.buscarPorUsername(username)
                .orElseThrow(() -> new NegocioException("El usuario no existe"));
    }

    /** Devuelve el id del cliente asociado a un usuario con rol CLIENTE. */
    public int clienteIdDe(String username) {
        Integer clienteId = buscarPorUsername(username).getClienteId();
        if (clienteId == null) {
            throw new NegocioException("Este usuario no esta asociado a un cliente");
        }
        return clienteId;
    }

    public String nombreDe(String username) {
        return usuarioDAO.buscarPorUsername(username).map(Usuario::getNombre).orElse(username);
    }
}
