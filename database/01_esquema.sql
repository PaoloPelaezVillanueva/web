-- =====================================================================
--  CRM WEB PARA LA GESTIÓN DE COBRANZAS - ESCALL PERÚ S.A.C.
--  Esquema de base de datos para MySQL 8
-- ---------------------------------------------------------------------
--  Cómo usarlo (MySQL Workbench):
--    1. Abre tu conexión local (ej: "Local instance MySQL80").
--    2. File -> Open SQL Script... -> elige este archivo.
--    3. Presiona el rayo (Execute) para ejecutar TODO el script.
--
--  Crea la base de datos "crm_cobranzas" y sus tablas. Es seguro
--  ejecutarlo varias veces: no borra nada que ya exista.
--
--  Los usuarios iniciales y el árbol de tipificaciones los crea el
--  backend al arrancar por primera vez (contraseñas cifradas con BCrypt).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS crm_cobranzas
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE crm_cobranzas;

-- ---------------------------------------------------------------------
-- 1. USUARIOS: administradores, supervisores y asesores.
--    Un asesor pertenece al equipo de un supervisor (supervisor_id).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    usuario         VARCHAR(40)  NOT NULL,
    nombre          VARCHAR(120) NOT NULL,
    email           VARCHAR(150) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    rol             VARCHAR(20)  NOT NULL,
    supervisor_id   BIGINT,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_usuario UNIQUE (usuario),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT fk_usuarios_supervisor FOREIGN KEY (supervisor_id) REFERENCES usuarios (id),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ADMIN', 'SUPERVISOR', 'ASESOR'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 2. CLIENTES (identificados por documento) y sus TELÉFONOS
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS clientes (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    tipo_documento  VARCHAR(10)  NOT NULL DEFAULT 'DNI',
    documento       VARCHAR(20)  NOT NULL,
    nombres         VARCHAR(150) NOT NULL,
    email           VARCHAR(150),
    direccion       VARCHAR(300),
    creado_en       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_clientes PRIMARY KEY (id),
    CONSTRAINT uk_clientes_documento UNIQUE (documento),
    INDEX idx_clientes_nombres (nombres)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS telefonos (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    cliente_id      BIGINT       NOT NULL,
    numero          VARCHAR(20)  NOT NULL,
    tipo            VARCHAR(20)  NOT NULL DEFAULT 'CELULAR',
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_telefonos PRIMARY KEY (id),
    CONSTRAINT uk_telefonos_cliente_numero UNIQUE (cliente_id, numero),
    CONSTRAINT fk_telefonos_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id) ON DELETE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 3. CUENTAS: obligación del cliente (operación, entidad, producto,
--    cartera, cosecha y deuda).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cuentas (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    cliente_id        BIGINT        NOT NULL,
    numero_operacion  VARCHAR(30)   NOT NULL,
    entidad           VARCHAR(100)  NOT NULL,
    producto          VARCHAR(100)  NOT NULL,
    cartera           VARCHAR(100)  NOT NULL,
    cosecha           VARCHAR(20)   NOT NULL,
    deuda_total       DECIMAL(12,2) NOT NULL,
    dias_mora         INT           NOT NULL DEFAULT 0,
    creado_en         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    actualizado_en    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_cuentas PRIMARY KEY (id),
    CONSTRAINT uk_cuentas_operacion UNIQUE (numero_operacion),
    CONSTRAINT fk_cuentas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT ck_cuentas_deuda CHECK (deuda_total >= 0),
    CONSTRAINT ck_cuentas_mora CHECK (dias_mora >= 0),
    INDEX idx_cuentas_cartera (cartera),
    INDEX idx_cuentas_cosecha (cosecha)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 4. CARGAS (Integración de Datos): historial de archivos procesados
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cargas (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    tipo              VARCHAR(20)   NOT NULL,
    nombre_archivo    VARCHAR(200)  NOT NULL,
    usuario_id        BIGINT        NOT NULL,
    total             INT           NOT NULL,
    aceptados         INT           NOT NULL,
    rechazados        INT           NOT NULL,
    detalle_errores   TEXT,
    creado_en         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_cargas PRIMARY KEY (id),
    CONSTRAINT fk_cargas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT ck_cargas_tipo CHECK (tipo IN ('ASIGNACION', 'PAGO'))
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 5. ASIGNACIONES: qué asesor gestiona cada cuenta y desde cuándo.
--    Solo una asignación activa por cuenta; las anteriores quedan como
--    historial (activa = FALSE, con fecha_fin).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS asignaciones (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    cuenta_id         BIGINT       NOT NULL,
    asesor_id         BIGINT       NOT NULL,
    fecha_asignacion  DATE         NOT NULL,
    fecha_fin         DATE,
    activa            BOOLEAN      NOT NULL DEFAULT TRUE,
    carga_id          BIGINT,
    CONSTRAINT pk_asignaciones PRIMARY KEY (id),
    CONSTRAINT fk_asignaciones_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT fk_asignaciones_asesor FOREIGN KEY (asesor_id) REFERENCES usuarios (id),
    CONSTRAINT fk_asignaciones_carga FOREIGN KEY (carga_id) REFERENCES cargas (id),
    INDEX idx_asignaciones_asesor_activa (asesor_id, activa),
    INDEX idx_asignaciones_cuenta_activa (cuenta_id, activa)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 6. ÁRBOL DE TIPIFICACIONES
--    Nivel 1 = tipo de contacto. Nivel 2 = resultado (depende del nivel 1)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tipificaciones_n1 (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    nombre          VARCHAR(80)  NOT NULL,
    es_contacto     BOOLEAN      NOT NULL DEFAULT FALSE,
    orden           INT          NOT NULL DEFAULT 0,
    activo          BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_tipificaciones_n1 PRIMARY KEY (id),
    CONSTRAINT uk_tipificaciones_n1_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS tipificaciones_n2 (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    n1_id                 BIGINT       NOT NULL,
    nombre                VARCHAR(80)  NOT NULL,
    requiere_compromiso   BOOLEAN      NOT NULL DEFAULT FALSE,
    orden                 INT          NOT NULL DEFAULT 0,
    activo                BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT pk_tipificaciones_n2 PRIMARY KEY (id),
    CONSTRAINT uk_tipificaciones_n2_nombre UNIQUE (n1_id, nombre),
    CONSTRAINT fk_tipificaciones_n2_n1 FOREIGN KEY (n1_id) REFERENCES tipificaciones_n1 (id)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 7. GESTIONES: cada interacción del asesor con una cuenta.
--    Se guardan N1 y N2 para conservar el historial exacto (RNF10).
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gestiones (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    cuenta_id             BIGINT        NOT NULL,
    asesor_id             BIGINT        NOT NULL,
    fecha_hora            DATETIME(6)   NOT NULL,
    telefono              VARCHAR(20)   NOT NULL,
    tipificacion_n1_id    BIGINT        NOT NULL,
    tipificacion_n2_id    BIGINT        NOT NULL,
    observacion           VARCHAR(500)  NOT NULL,
    creado_en             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_gestiones PRIMARY KEY (id),
    CONSTRAINT fk_gestiones_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT fk_gestiones_asesor FOREIGN KEY (asesor_id) REFERENCES usuarios (id),
    CONSTRAINT fk_gestiones_n1 FOREIGN KEY (tipificacion_n1_id) REFERENCES tipificaciones_n1 (id),
    CONSTRAINT fk_gestiones_n2 FOREIGN KEY (tipificacion_n2_id) REFERENCES tipificaciones_n2 (id),
    INDEX idx_gestiones_fecha (fecha_hora),
    INDEX idx_gestiones_asesor_fecha (asesor_id, fecha_hora)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 8. PROMESAS DE PAGO: nacen de una gestión cuya tipificación lo exige
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS promesas (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    gestion_id        BIGINT        NOT NULL,
    cuenta_id         BIGINT        NOT NULL,
    asesor_id         BIGINT        NOT NULL,
    fecha_compromiso  DATE          NOT NULL,
    monto             DECIMAL(12,2) NOT NULL,
    estado            VARCHAR(15)   NOT NULL DEFAULT 'PENDIENTE',
    creado_en         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_promesas PRIMARY KEY (id),
    CONSTRAINT uk_promesas_gestion UNIQUE (gestion_id),
    CONSTRAINT fk_promesas_gestion FOREIGN KEY (gestion_id) REFERENCES gestiones (id),
    CONSTRAINT fk_promesas_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT fk_promesas_asesor FOREIGN KEY (asesor_id) REFERENCES usuarios (id),
    CONSTRAINT ck_promesas_monto CHECK (monto > 0),
    CONSTRAINT ck_promesas_estado CHECK (estado IN ('PENDIENTE', 'CUMPLIDA')),
    INDEX idx_promesas_fecha (fecha_compromiso)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 9. PAGOS: cargados desde Integración de Datos.
--    La clave única evita registrar el mismo pago dos veces.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS pagos (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    cuenta_id       BIGINT        NOT NULL,
    fecha_pago      DATE          NOT NULL,
    monto           DECIMAL(12,2) NOT NULL,
    referencia      VARCHAR(60)   NOT NULL DEFAULT '',
    carga_id        BIGINT,
    creado_en       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_pagos PRIMARY KEY (id),
    CONSTRAINT uk_pagos_duplicado UNIQUE (cuenta_id, fecha_pago, monto, referencia),
    CONSTRAINT fk_pagos_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
    CONSTRAINT fk_pagos_carga FOREIGN KEY (carga_id) REFERENCES cargas (id),
    CONSTRAINT ck_pagos_monto CHECK (monto > 0),
    INDEX idx_pagos_fecha (fecha_pago)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 10. AUDITORÍA: acciones críticas (RF23)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS auditoria (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    usuario         VARCHAR(40)   NOT NULL,
    accion          VARCHAR(60)   NOT NULL,
    entidad         VARCHAR(60)   NOT NULL,
    entidad_id      VARCHAR(40),
    detalle         VARCHAR(1000),
    fecha           DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_auditoria PRIMARY KEY (id),
    INDEX idx_auditoria_fecha (fecha)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- 11. VISTA DE REPORTE: productividad por asesor (útil para análisis)
--     SELECT * FROM vista_productividad_asesor;
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW vista_productividad_asesor AS
SELECT u.id                                   AS asesor_id,
       u.nombre                               AS asesor,
       COUNT(g.id)                            AS gestiones,
       SUM(CASE WHEN n1.es_contacto THEN 1 ELSE 0 END) AS contactos,
       COUNT(p.id)                            AS promesas,
       COALESCE(SUM(p.monto), 0)              AS monto_prometido
FROM usuarios u
LEFT JOIN gestiones g          ON g.asesor_id = u.id
LEFT JOIN tipificaciones_n1 n1 ON n1.id = g.tipificacion_n1_id
LEFT JOIN promesas p           ON p.gestion_id = g.id
WHERE u.rol = 'ASESOR'
GROUP BY u.id, u.nombre;

-- ---------------------------------------------------------------------
-- 12. (OPCIONAL, RECOMENDADO) Usuario propio para la aplicación.
--     Más seguro que usar "root". Quita los "-- ", cambia la contraseña,
--     ejecuta y usa esos datos en backend/.env
-- ---------------------------------------------------------------------
-- CREATE USER IF NOT EXISTS 'crm_app'@'localhost' IDENTIFIED BY 'CambiaEstaClave2026';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON crm_cobranzas.* TO 'crm_app'@'localhost';
-- FLUSH PRIVILEGES;
