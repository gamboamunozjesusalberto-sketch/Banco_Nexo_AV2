package com.banco.service;

import com.banco.dao.ClienteDAO;
import com.banco.dao.factory.DAOFactory;
import com.banco.model.Cliente;
import com.banco.model.Rol;
import com.banco.model.TipoCuenta;
import com.banco.util.NegocioException;
import com.banco.util.TransaccionJdbc;
import com.banco.util.Validaciones;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Capa de LOGICA DE NEGOCIO para clientes: validacion de datos y control de duplicados.
 * Obtiene su DAO desde la fabrica (Factory) y solo conoce la interfaz (SOLID - D).
 */
@Service
public class ClienteService {

    private final ClienteDAO clienteDAO = DAOFactory.getClienteDAO();
    private final UsuarioService usuarioService;
    private final CuentaService cuentaService;

    public ClienteService(UsuarioService usuarioService, CuentaService cuentaService) {
        this.usuarioService = usuarioService;
        this.cuentaService = cuentaService;
    }

    /**
     * Registro en linea (para que los alumnos se creen una cuenta en la demo de clase):
     * crea el cliente, su usuario (DNI) y una cuenta de ahorros en S/ 0.00, todo en una transaccion.
     * La clave debe ser segura (8 caracteres, letras y numeros).
     */
    public Cliente registrarseEnLinea(Cliente cliente, String password, String confirmacion) {
        if (!Validaciones.passwordSegura(password)) {
            throw new NegocioException("La clave debe tener al menos 8 caracteres, con letras y numeros");
        }
        if (!password.equals(confirmacion)) {
            throw new NegocioException("Las claves no coinciden");
        }
        return TransaccionJdbc.ejecutar(() -> {
            registrarConAcceso(cliente, password, false);
            cuentaService.abrirCuenta(cliente.getId(), TipoCuenta.AHORROS, BigDecimal.ZERO);
            return cliente;
        });
    }

    /**
     * El cajero registra al cliente y se crea su usuario de acceso (usuario = DNI).
     * Ambas cosas van en una sola TRANSACCION: si falla el usuario, tampoco se guarda el cliente.
     */
    public Cliente registrarConAcceso(Cliente cliente, String password) {
        return registrarConAcceso(cliente, password, true);
    }

    /** @param temporal true si la clave la puso el cajero: el cliente la cambia en su primer ingreso. */
    public Cliente registrarConAcceso(Cliente cliente, String password, boolean temporal) {
        return TransaccionJdbc.ejecutar(() -> {
            registrar(cliente);
            usuarioService.crear(cliente.getDni(), password, cliente.getNombre(), Rol.CLIENTE, cliente.getId(), temporal);
            return cliente;
        });
    }

    public void registrar(Cliente c) {
        if (Validaciones.vacio(c.getNombre())) {
            throw new NegocioException("El nombre es obligatorio");
        }
        if (!Validaciones.dniValido(c.getDni())) {
            throw new NegocioException("El DNI debe tener exactamente 8 digitos");
        }
        if (!Validaciones.correoValido(c.getCorreo())) {
            throw new NegocioException("El correo no tiene un formato valido");
        }
        if (!Validaciones.telefonoValido(c.getTelefono())) {
            throw new NegocioException("El telefono debe tener 9 digitos");
        }
        if (clienteDAO.existePorDni(c.getDni())) {
            throw new NegocioException("Ya existe un cliente registrado con ese DNI");
        }
        c.setNombre(c.getNombre().trim());
        if (Validaciones.vacio(c.getTelefono())) {
            c.setTelefono(null);
        }
        clienteDAO.insertar(c);
    }

    /** Clientes reales (sin el registro interno del banco que es duenio de la cuenta de recaudacion). */
    public List<Cliente> listar() {
        return clienteDAO.listar().stream()
                .filter(c -> !CuentaService.DNI_RECAUDADORA.equals(c.getDni())).toList();
    }

    public Cliente buscar(int id) {
        return clienteDAO.buscarPorId(id).orElseThrow(() -> new NegocioException("El cliente no existe"));
    }
}
