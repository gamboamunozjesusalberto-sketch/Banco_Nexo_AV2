package com.banco.config;

import com.banco.dao.factory.DAOFactory;
import com.banco.model.Rol;
import com.banco.service.CuentaService;
import com.banco.service.UsuarioService;
import com.banco.util.ConexionBD;
import com.banco.util.DatabaseException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Al arrancar:
 *  1. Si todavia no existe ningun usuario, crea uno por cada rol del personal del banco
 *     (claves cifradas con BCrypt y marcadas como temporales: se cambian en el primer ingreso).
 *  2. Crea la cuenta interna "NEXO Recaudacion" si falta (ahi entra lo cobrado por pago de servicios).
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioService usuarioService;
    private final CuentaService cuentaService;

    public DatosIniciales(UsuarioService usuarioService, CuentaService cuentaService) {
        this.usuarioService = usuarioService;
        this.cuentaService = cuentaService;
    }

    @Override
    public void run(String... args) {
        try {
            if (DAOFactory.getUsuarioDAO().contar() == 0) {
                usuarioService.crearPersonal("admin", "admin123", "Administrador del sistema", Rol.ADMINISTRADOR);
                usuarioService.crearPersonal("asesor", "asesor123", "Asesor de credito", Rol.ASESOR);
                usuarioService.crearPersonal("cajero", "cajero123", "Cajero de ventanilla", Rol.CAJERO);
                usuarioService.crearPersonal("auditor", "auditor123", "Auditor interno", Rol.AUDITOR);
                System.out.println(">>> Usuarios iniciales creados: admin, asesor, cajero y auditor (ver INSTRUCCIONES.md).");
            }
            cuentaService.asegurarCuentaRecaudadora();
            System.out.println(">>> Banco NEXO listo.");
        } catch (DatabaseException | IllegalStateException | com.zaxxer.hikari.pool.HikariPool.PoolInitializationException e) {
            System.err.println(">>> No se pudo preparar la base de datos. Revisa la variable DB_PASSWORD, la conexion "
                    + "a Supabase y que hayas ejecutado sql/migracion_avance2.sql (o sql/schema.sql en una base nueva).");
            System.err.println(">>> Detalle: " + e.getMessage());
        } finally {
            // fuera de una peticion web no pasa el FiltroConexion: devolvemos la conexion al pool aqui
            try {
                ConexionBD.liberarSiExiste();
            } catch (RuntimeException ignorada) {
                // si no hubo conexion no hay nada que liberar
            }
        }
    }
}
