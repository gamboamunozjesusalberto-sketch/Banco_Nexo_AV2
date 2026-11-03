-- =====================================================================
-- Banco NEXO - Script completo de la base de datos (segundo avance)
-- Ejecutar en el SQL Editor de Supabase (o en pgAdmin) sobre una base VACIA.
-- Si ya tienes las tablas del primer avance, usa sql/migracion_avance2.sql.
-- Los usuarios iniciales (admin, asesor, cajero, auditor) los crea la aplicacion al arrancar.
-- =====================================================================

DROP TABLE IF EXISTS parametro_historial;
DROP TABLE IF EXISTS operacion_caja;
DROP TABLE IF EXISTS alerta_riesgo;
DROP TABLE IF EXISTS notificacion;
DROP TABLE IF EXISTS bitacora_acceso;
DROP TABLE IF EXISTS solicitud_credito;
DROP TABLE IF EXISTS transaccion;
DROP TABLE IF EXISTS cuenta;
DROP TABLE IF EXISTS usuario;
DROP TABLE IF EXISTS cliente;
DROP TABLE IF EXISTS parametro;
DROP SEQUENCE IF EXISTS seq_numero_cuenta;

CREATE SEQUENCE seq_numero_cuenta START 1000000001;

CREATE TABLE parametro (
    clave        VARCHAR(40)  PRIMARY KEY,
    valor        VARCHAR(40)  NOT NULL,
    descripcion  VARCHAR(150) NOT NULL
);

INSERT INTO parametro (clave, valor, descripcion) VALUES
    ('LIMITE_OPERACION_DEFECTO', '5000', 'Limite por operacion (S/) que se asigna a cada cuenta nueva'),
    ('MONTO_MAXIMO_CREDITO', '100000', 'Monto maximo (S/) que un cliente puede pedir en una solicitud de credito'),
    ('TASA_INTERES_ANUAL', '18', 'Tasa de interes anual (%) para calcular las cuotas de los creditos'),
    ('UMBRAL_MONTO_ALTO', '3000', 'Desde este monto (S/) se pide confirmar con clave y se genera una alerta de riesgo'),
    ('MAX_INTENTOS_FALLIDOS', '3', 'Intentos de ingreso fallidos seguidos que generan una alerta de riesgo'),
    ('MAX_OPERACIONES_VENTANA', '5', 'Operaciones de una misma cuenta dentro de la ventana que generan una alerta'),
    ('VENTANA_MINUTOS', '10', 'Ventana de tiempo (minutos) para contar operaciones seguidas de una misma cuenta'),
    ('MINUTOS_BLOQUEO', '15', 'Minutos en que se cuentan los intentos fallidos y que dura el bloqueo del usuario');

CREATE TABLE cliente (
    id        SERIAL PRIMARY KEY,
    nombre    VARCHAR(100) NOT NULL,
    dni       VARCHAR(8)   NOT NULL UNIQUE,
    correo    VARCHAR(100) NOT NULL,
    telefono  VARCHAR(9)
);

CREATE TABLE usuario (
    id             SERIAL PRIMARY KEY,
    username       VARCHAR(30)  NOT NULL UNIQUE,
    password_hash  VARCHAR(100) NOT NULL,
    nombre         VARCHAR(100) NOT NULL,
    rol            VARCHAR(20)  NOT NULL CHECK (rol IN ('ADMINISTRADOR', 'CLIENTE', 'ASESOR', 'CAJERO', 'AUDITOR')),
    activo         BOOLEAN      NOT NULL DEFAULT TRUE,
    debe_cambiar_clave BOOLEAN  NOT NULL DEFAULT FALSE,
    cliente_id     INT UNIQUE REFERENCES cliente(id)
);

CREATE TABLE cuenta (
    id                SERIAL PRIMARY KEY,
    numero            VARCHAR(20)   NOT NULL UNIQUE,
    tipo              VARCHAR(10)   NOT NULL CHECK (tipo IN ('AHORROS', 'CORRIENTE')),
    saldo             NUMERIC(14,2) NOT NULL DEFAULT 0,
    limite_operacion  NUMERIC(14,2) NOT NULL CHECK (limite_operacion > 0),
    cliente_id        INT           NOT NULL REFERENCES cliente(id),
    fecha_apertura    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transaccion (
    id                SERIAL PRIMARY KEY,
    cuenta_id         INT           NOT NULL REFERENCES cuenta(id),
    tipo              VARCHAR(25)   NOT NULL CHECK (tipo IN
                      ('DEPOSITO', 'RETIRO', 'TRANSFERENCIA_ENVIADA', 'TRANSFERENCIA_RECIBIDA', 'PAGO_SERVICIO',
                       'DESEMBOLSO_CREDITO', 'EXTORNO_DEPOSITO', 'EXTORNO_RETIRO', 'RECAUDACION_SERVICIO')),
    monto             NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    saldo_resultante  NUMERIC(14,2) NOT NULL,
    fecha             TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    descripcion       VARCHAR(150)
);

CREATE TABLE solicitud_credito (
    id                 SERIAL PRIMARY KEY,
    cliente_id         INT           NOT NULL REFERENCES cliente(id),
    monto              NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    plazo_meses        INT           NOT NULL CHECK (plazo_meses > 0),
    motivo             VARCHAR(200)  NOT NULL,
    estado             VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE'
                       CHECK (estado IN ('PENDIENTE', 'EN_EVALUACION', 'APROBADA', 'DESEMBOLSADA', 'RECHAZADA')),
    observaciones      VARCHAR(250),
    asesor_id          INT REFERENCES usuario(id),
    cuenta_destino_id  INT REFERENCES cuenta(id),
    tasa_anual         NUMERIC(5,2),
    fecha_solicitud    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_resolucion   TIMESTAMP
);

CREATE TABLE bitacora_acceso (
    id        SERIAL PRIMARY KEY,
    username  VARCHAR(30)  NOT NULL,
    evento    VARCHAR(20)  NOT NULL,
    detalle   VARCHAR(150),
    fecha     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Patron OBSERVER: avisos para el cliente (credito desembolsado, transferencia recibida...)
CREATE TABLE notificacion (
    id          SERIAL PRIMARY KEY,
    cliente_id  INT          NOT NULL REFERENCES cliente(id),
    titulo      VARCHAR(80)  NOT NULL,
    mensaje     VARCHAR(250) NOT NULL,
    leida       BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Patron OBSERVER: alertas para el auditor (monto alto, intentos fallidos, muchas operaciones seguidas)
CREATE TABLE alerta_riesgo (
    id          SERIAL PRIMARY KEY,
    tipo        VARCHAR(30)  NOT NULL CHECK (tipo IN ('MONTO_ALTO', 'INTENTOS_FALLIDOS', 'OPERACIONES_SEGUIDAS')),
    nivel       VARCHAR(10)  NOT NULL CHECK (nivel IN ('MEDIO', 'ALTO')),
    detalle     VARCHAR(250) NOT NULL,
    referencia  VARCHAR(40),
    revisada    BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Patron COMMAND: historial de cada orden de caja (deposito o retiro) y de sus extornos
CREATE TABLE operacion_caja (
    id              SERIAL PRIMARY KEY,
    comando         VARCHAR(10)   NOT NULL CHECK (comando IN ('DEPOSITO', 'RETIRO')),
    cuenta_id       INT           NOT NULL REFERENCES cuenta(id),
    monto           NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    cajero          VARCHAR(30)   NOT NULL,
    transaccion_id  INT REFERENCES transaccion(id),
    estado          VARCHAR(10)   NOT NULL CHECK (estado IN ('EJECUTADA', 'EXTORNADA')),
    extorno_id      INT REFERENCES transaccion(id),
    extornado_por   VARCHAR(30),
    motivo_extorno  VARCHAR(150),
    fecha           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Patron MEMENTO: cada version guardada de la configuracion de parametros
CREATE TABLE parametro_historial (
    id           SERIAL PRIMARY KEY,
    valores      TEXT         NOT NULL,
    usuario      VARCHAR(30)  NOT NULL,
    descripcion  VARCHAR(150) NOT NULL,
    fecha        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seguridad en Supabase: RLS activo en todas las tablas (la app entra por JDBC como duena de las tablas)
ALTER TABLE parametro ENABLE ROW LEVEL SECURITY;
ALTER TABLE cliente ENABLE ROW LEVEL SECURITY;
ALTER TABLE usuario ENABLE ROW LEVEL SECURITY;
ALTER TABLE cuenta ENABLE ROW LEVEL SECURITY;
ALTER TABLE transaccion ENABLE ROW LEVEL SECURITY;
ALTER TABLE solicitud_credito ENABLE ROW LEVEL SECURITY;
ALTER TABLE bitacora_acceso ENABLE ROW LEVEL SECURITY;
ALTER TABLE notificacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE alerta_riesgo ENABLE ROW LEVEL SECURITY;
ALTER TABLE operacion_caja ENABLE ROW LEVEL SECURITY;
ALTER TABLE parametro_historial ENABLE ROW LEVEL SECURITY;
