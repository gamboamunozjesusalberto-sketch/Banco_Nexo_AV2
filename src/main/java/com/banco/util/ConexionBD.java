package com.banco.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * PATRON CREACIONAL: SINGLETON.
 * Garantiza un UNICO administrador de conexiones a la base de datos en todo el sistema.
 *
 * Primer avance: guardaba una sola conexion, asi que se atendia una operacion a la vez.
 * Segundo avance: el Singleton guarda un POOL de conexiones (HikariCP). Cada peticion web toma una
 * conexion del pool, la usa y la devuelve al terminar (FiltroConexion). Asi varios usuarios operan
 * al mismo tiempo, sin abrir una conexion nueva por cada consulta. Los DAO no cambiaron: siguen
 * pidiendo ConexionBD.getInstancia().getConexion().
 *
 * La URL y el usuario se leen de application.properties (db.url, db.user).
 * La clave se lee de la variable de entorno DB_PASSWORD, para no publicarla en el codigo.
 * Base de datos: PostgreSQL en Supabase (Session pooler, conexion cifrada con SSL).
 */
public final class ConexionBD {

    private static final String ZONA_HORARIA = "America/Lima";
    private static final int CONEXIONES_MAXIMAS = 5;

    private static ConexionBD instancia;

    private final HikariDataSource pool;
    /** Conexion que esta usando cada hilo (cada peticion web tiene su propio hilo). */
    private final ThreadLocal<Connection> conexionDelHilo = new ThreadLocal<>();

    private ConexionBD() {
        Properties props = new Properties();
        try (InputStream in = ConexionBD.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new IllegalStateException("No se encontro application.properties");
            }
            props.load(in);
        } catch (IOException e) {
            throw new DatabaseException("No se pudo leer application.properties", e);
        }
        // La clave NO se guarda en el archivo: se lee de la variable de entorno DB_PASSWORD.
        // Si la variable no existe, se usa db.password (util para pruebas locales).
        String claveEntorno = System.getenv("DB_PASSWORD");
        String clave = (claveEntorno != null && !claveEntorno.isBlank())
                ? claveEntorno
                : props.getProperty("db.password");
        if (clave == null || clave.isBlank()) {
            throw new IllegalStateException("Falta la clave de la base de datos: define la variable de entorno DB_PASSWORD");
        }
        HikariConfig config = new HikariConfig();
        // DB_URL y DB_USER son opcionales: permiten apuntar a otra base (por ejemplo una local) sin tocar el archivo
        config.setJdbcUrl(entornoO("DB_URL", props.getProperty("db.url")));
        config.setUsername(entornoO("DB_USER", props.getProperty("db.user")));
        config.setPassword(clave);
        config.setMaximumPoolSize(CONEXIONES_MAXIMAS);
        config.setMinimumIdle(1);
        config.setPoolName("NexoPool");
        // Supabase trabaja en hora UTC: cada conexion usa la hora de Peru para que las fechas salgan bien
        config.setConnectionInitSql("SET TIME ZONE '" + ZONA_HORARIA + "'");
        this.pool = new HikariDataSource(config);
    }

    private static String entornoO(String variable, String porDefecto) {
        String valor = System.getenv(variable);
        return (valor != null && !valor.isBlank()) ? valor : porDefecto;
    }

    /** Punto de acceso global a la unica instancia. */
    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    /**
     * Devuelve la conexion del hilo actual. La primera vez en una peticion la toma del pool;
     * las siguientes consultas de la misma peticion (y de su transaccion) reutilizan la misma.
     */
    public Connection getConexion() {
        try {
            Connection actual = conexionDelHilo.get();
            if (actual == null || actual.isClosed()) {
                actual = pool.getConnection();
                conexionDelHilo.set(actual);
            }
            return actual;
        } catch (SQLException e) {
            throw new DatabaseException("No se pudo conectar a la base de datos: " + e.getMessage(), e);
        }
    }

    /** Devuelve la conexion del hilo al pool (lo llama FiltroConexion al terminar cada peticion). */
    /**
     * Devuelve al pool la conexion del hilo actual, si es que se llego a crear el Singleton.
     * Asi una peticion que no uso la base (una imagen, el CSS) no obliga a abrir el pool.
     */
    public static void liberarSiExiste() {
        ConexionBD actual;
        synchronized (ConexionBD.class) {
            actual = instancia;
        }
        if (actual != null) {
            actual.liberar();
        }
    }

    public void liberar() {
        Connection actual = conexionDelHilo.get();
        conexionDelHilo.remove();
        if (actual == null) {
            return;
        }
        try {
            if (!actual.getAutoCommit()) { // una transaccion quedo a medias: se revierte
                actual.rollback();
                actual.setAutoCommit(true);
            }
        } catch (SQLException ignorada) {
            // el pool descartara la conexion si quedo inservible
        } finally {
            try {
                actual.close(); // en un pool, close() no cierra: devuelve la conexion
            } catch (SQLException ignorada) {
                // nada mas que hacer
            }
        }
    }
}
