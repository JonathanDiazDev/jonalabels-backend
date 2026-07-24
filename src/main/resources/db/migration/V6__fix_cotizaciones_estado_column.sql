-- V6__fix_cotizaciones_estado_column.sql
-- V5 created both `estado` and `estado_cotizacion` columns.
-- The JPA entity only maps `estado` (EstadoCotizacion enum).
-- `estado_cotizacion` has NOT NULL with no default, causing INSERT failures.
-- This migration drops the redundant column.

ALTER TABLE cotizaciones DROP COLUMN IF EXISTS estado_cotizacion;
