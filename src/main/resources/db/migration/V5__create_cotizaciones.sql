-- V5__create_cotizaciones.sql

CREATE TABLE cotizaciones
(
    id              BIGSERIAL PRIMARY KEY,
    nombre          VARCHAR(150) NOT NULL,
    whatsapp        VARCHAR(20) NOT NULL,
    email           VARCHAR(200),
    cantidad        INT,
    medidas         VARCHAR(100),
    url_archivo     VARCHAR(500),
    url_diseno      VARCHAR(500), -- Faltaba esta
    estado          VARCHAR(50),  -- Faltaba esta
    fecha_creacion  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado_cotizacion VARCHAR(50) NOT NULL
);
