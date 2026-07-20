-- V2__resena_estado_moderacion.sql

ALTER TABLE resenas RENAME COLUMN estado TO estado_moderacion;

ALTER TABLE resenas ALTER COLUMN estado_moderacion SET DEFAULT 'PENDIENTE';

UPDATE resenas SET estado_moderacion = 'PENDIENTE' WHERE estado_moderacion = 'PENDIENTE_APROBACION';
