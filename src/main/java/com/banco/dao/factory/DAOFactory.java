package com.banco.dao.factory;

import com.banco.dao.AlertaRiesgoDAO;
import com.banco.dao.BitacoraDAO;
import com.banco.dao.NotificacionDAO;
import com.banco.dao.OperacionCajaDAO;
import com.banco.dao.ParametroHistorialDAO;
import com.banco.dao.ClienteDAO;
import com.banco.dao.CuentaDAO;
import com.banco.dao.ParametroDAO;
import com.banco.dao.SolicitudCreditoDAO;
import com.banco.dao.TransaccionDAO;
import com.banco.dao.UsuarioDAO;
import com.banco.dao.impl.AlertaRiesgoDAOImpl;
import com.banco.dao.impl.BitacoraDAOImpl;
import com.banco.dao.impl.NotificacionDAOImpl;
import com.banco.dao.impl.OperacionCajaDAOImpl;
import com.banco.dao.impl.ParametroHistorialDAOImpl;
import com.banco.dao.impl.ClienteDAOImpl;
import com.banco.dao.impl.CuentaDAOImpl;
import com.banco.dao.impl.ParametroDAOImpl;
import com.banco.dao.impl.SolicitudCreditoDAOImpl;
import com.banco.dao.impl.TransaccionDAOImpl;
import com.banco.dao.impl.UsuarioDAOImpl;

/**
 * PATRON CREACIONAL: FACTORY.
 * Crea el DAO correcto para cada entidad. Los servicios piden un DAO a la fabrica y
 * solo conocen la interfaz, asi no quedan acoplados a las clases JDBC concretas.
 * Si manana se cambia JDBC por otra tecnologia, solo se modifica esta clase.
 */
public final class DAOFactory {

    private DAOFactory() {
    }

    public static ClienteDAO getClienteDAO() { return new ClienteDAOImpl(); }

    public static CuentaDAO getCuentaDAO() { return new CuentaDAOImpl(); }

    public static TransaccionDAO getTransaccionDAO() { return new TransaccionDAOImpl(); }

    public static UsuarioDAO getUsuarioDAO() { return new UsuarioDAOImpl(); }

    public static SolicitudCreditoDAO getSolicitudCreditoDAO() { return new SolicitudCreditoDAOImpl(); }

    public static BitacoraDAO getBitacoraDAO() { return new BitacoraDAOImpl(); }

    public static ParametroDAO getParametroDAO() { return new ParametroDAOImpl(); }

    // ---- segundo avance ----
    public static NotificacionDAO getNotificacionDAO() { return new NotificacionDAOImpl(); }

    public static AlertaRiesgoDAO getAlertaRiesgoDAO() { return new AlertaRiesgoDAOImpl(); }

    public static OperacionCajaDAO getOperacionCajaDAO() { return new OperacionCajaDAOImpl(); }

    public static ParametroHistorialDAO getParametroHistorialDAO() { return new ParametroHistorialDAOImpl(); }
}
