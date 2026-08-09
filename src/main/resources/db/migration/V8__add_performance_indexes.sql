-- V8__add_performance_indexes.sql
-- Índices de rendimiento para consultas frecuentes del dashboard y vistas de moderación.

-- Cotizaciones: filtrado por estado y ordenado por fecha (dashboard)
CREATE INDEX IF NOT EXISTS idx_cotizaciones_estado_fecha
    ON cotizaciones (estado, fecha_creacion DESC);

-- Reseñas: consulta de reseñas aprobadas (públicas) y pendientes (moderación)
CREATE INDEX IF NOT EXISTS idx_resenas_estado_moderacion
    ON resenas (estado_moderacion);

-- Pedidos: joins por cada clave foránea
CREATE INDEX IF NOT EXISTS idx_pedidos_usuario_id  ON pedidos (usuario_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_producto_id ON pedidos (producto_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_diseno_id   ON pedidos (diseno_id);
CREATE INDEX IF NOT EXISTS idx_pedidos_taller_id   ON pedidos (taller_id);

-- Disenos: joins por usuario (galería de diseños del cliente)
CREATE INDEX IF NOT EXISTS idx_disenos_usuario_id  ON disenos (usuario_id);
