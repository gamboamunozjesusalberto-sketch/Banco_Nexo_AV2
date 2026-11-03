-- =====================================================================
-- Banco NEXO - Migracion del primer al segundo avance.
-- Ejecutar UNA sola vez en el SQL Editor de Supabase sobre las tablas del primer avance.
-- No borra datos: solo agrega columnas, tablas y parametros nuevos.
-- =====================================================================

-- Nuevos parametros (si ya existen, no se tocan)
INSERT INTO parametro (clave, valor, descripcion) VALUES
    ('TASA_INTERES_ANUAL', '18', 'Tasa de interes anual (%) para calcular las cuotas de los creditos'),
    ('UMBRAL_MONTO_ALTO', '3000', 'Desde este monto (S/) se pide confirmar con clave y se genera una alerta de riesgo'),
    ('MAX_INTENTOS_FALLIDOS', '3', 'Intentos de ingreso fallidos seguidos que generan una alerta de riesgo'),
    ('MAX_OPERACIONES_VENTANA', '5', 'Operaciones de una misma cuenta dentro de la ventana que generan una alerta'),
    ('VENTANA_MINUTOS', '10', 'Ventana de tiempo (minutos) para contar operaciones seguidas de una misma cuenta'),
    ('MINUTOS_BLOQUEO', '15', 'Minutos en que se cuentan los intentos fallidos y que dura el bloqueo del usuario')
ON CONFLICT (clave) DO NOTHING;

-- Clave temporal: obliga a cambiarla en el siguiente ingreso
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS debe_cambiar_clave BOOLEAN NOT NULL DEFAULT FALSE;

-- Nuevos tipos de movimiento
ALTER TABLE transaccion DROP CONSTRAINT IF EXISTS transaccion_tipo_check;
ALTER TABLE transaccion ADD CONSTRAINT transaccion_tipo_check CHECK (tipo IN
    ('DEPOSITO', 'RETIRO', 'TRANSFERENCIA_ENVIADA', 'TRANSFERENCIA_RECIBIDA', 'PAGO_SERVICIO',
     'DESEMBOLSO_CREDITO', 'EXTORNO_DEPOSITO', 'EXTORNO_RETIRO', 'RECAUDACION_SERVICIO'));

-- Credito: nuevos estados (patron State), cuenta de desembolso y tasa
ALTER TABLE solicitud_credito ALTER COLUMN estado TYPE VARCHAR(15);
ALTER TABLE solicitud_credito DROP CONSTRAINT IF EXISTS solicitud_credito_estado_check;
ALTER TABLE solicitud_credito ADD CONSTRAINT solicitud_credito_estado_check
    CHECK (estado IN ('PENDIENTE', 'EN_EVALUACION', 'APROBADA', 'DESEMBOLSADA', 'RECHAZADA'));
ALTER TABLE solicitud_credito ADD COLUMN IF NOT EXISTS cuenta_destino_id INT REFERENCES cuenta(id);
ALTER TABLE solicitud_credito ADD COLUMN IF NOT EXISTS tasa_anual NUMERIC(5,2);

CREATE TABLE IF NOT EXISTS notificacion (
    id          SERIAL PRIMARY KEY,
    cliente_id  INT          NOT NULL REFERENCES cliente(id),
    titulo      VARCHAR(80)  NOT NULL,
    mensaje     VARCHAR(250) NOT NULL,
    leida       BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS alerta_riesgo (
    id          SERIAL PRIMARY KEY,
    tipo        VARCHAR(30)  NOT NULL CHECK (tipo IN ('MONTO_ALTO', 'INTENTOS_FALLIDOS', 'OPERACIONES_SEGUIDAS')),
    nivel       VARCHAR(10)  NOT NULL CHECK (nivel IN ('MEDIO', 'ALTO')),
    detalle     VARCHAR(250) NOT NULL,
    referencia  VARCHAR(40),
    revisada    BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS operacion_caja (
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

CREATE TABLE IF NOT EXISTS parametro_historial (
    id           SERIAL PRIMARY KEY,
    valores      TEXT         NOT NULL,
    usuario      VARCHAR(30)  NOT NULL,
    descripcion  VARCHAR(150) NOT NULL,
    fecha        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE notificacion ENABLE ROW LEVEL SECURITY;
ALTER TABLE alerta_riesgo ENABLE ROW LEVEL SECURITY;
ALTER TABLE operacion_caja ENABLE ROW LEVEL SECURITY;
ALTER TABLE parametro_historial ENABLE ROW LEVEL SECURITY;
