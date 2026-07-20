-- V1__init_schema.sql

CREATE TABLE usuarios
(
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    rol                 VARCHAR(50)  NOT NULL, -- Valores esperados: ADMIN, CLIENTE
    direccion_envio     TEXT,
    fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE productos
(
    id                     BIGSERIAL PRIMARY KEY,
    nombre                 VARCHAR(100) NOT NULL, -- Ej: "Etiqueta de Satín Premium"
    tipo_material          VARCHAR(100) NOT NULL,
    descripcion_corta      VARCHAR(255),
    descripcion_detallada  TEXT,
    -- Rutas a los assets (SVG/PNG) que usaremos en Framer Motion para el frontend
    recurso_capa_base      TEXT,
    recurso_capa_acabado   TEXT,
    precio_base_referencia DECIMAL(10, 2),        -- Precio "Desde $X" puramente informativo
    fecha_creacion         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion    TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    calificacion_promedio  DECIMAL(3, 2) DEFAULT 0.00,
    total_resenas          INT           DEFAULT 0
);

CREATE TABLE talleres
(
    id                     BIGSERIAL PRIMARY KEY,
    nombre_contacto        VARCHAR(255) NOT NULL,
    telefono               VARCHAR(50),
    costo_maquila_estimado DECIMAL(10, 2),
    fecha_creacion         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE disenos
(
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios (id),
    url_archivo_logo    TEXT   NOT NULL, -- Enlace al S3/Bucket donde se guardó el logo del cliente
    notas_cliente       TEXT,            -- Ej: "Lo necesito a 3 colores, el fondo debe ser negro"
    fecha_creacion      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE pedidos
(
    id                    BIGSERIAL PRIMARY KEY,
    usuario_id            BIGINT      NOT NULL REFERENCES usuarios (id),
    producto_id           BIGINT      NOT NULL REFERENCES productos (id),
    diseno_id             BIGINT      NOT NULL REFERENCES disenos (id),
    taller_id             BIGINT REFERENCES talleres (id), -- Es nulo al inicio, el admin lo asigna luego
    estado                VARCHAR(50) NOT NULL,            -- ESPERANDO_FACTIBILIDAD, COTIZADO, RECHAZADO, PAGADO, EN_PRODUCCION, FINALIZADO
    cantidad              INT         NOT NULL,
    precio_final_cotizado DECIMAL(10, 2),                  -- Lo que el cliente pagará
    costo_taller_acordado DECIMAL(10, 2),                  -- Lo que te cobra el taller (vital para calcular tu margen neto)
    comentarios_admin     TEXT,                            -- Notas para el cliente (ej. ajustes al logo)
    fecha_creacion        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE resenas
(
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios (id),
    producto_id         BIGINT NOT NULL REFERENCES productos (id),
    pedido_id           BIGINT NOT NULL UNIQUE REFERENCES pedidos (id), -- Una reseña por pedido
    calificacion        INT    NOT NULL CHECK (calificacion >= 1 AND calificacion <= 5),
    comentario          TEXT,
    estado              VARCHAR(50) DEFAULT 'PENDIENTE_APROBACION',     -- Para que moderes antes de publicar
    fecha_creacion      TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP   DEFAULT CURRENT_TIMESTAMP
);