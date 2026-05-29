-- Extensión de ofertas/recompensas para gestión completa del comercio
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS categoria VARCHAR(50);

ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS tipo VARCHAR(20) DEFAULT 'producto_gratis';
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS porcentaje_descuento INT;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS monto_maximo_descuento DECIMAL(10, 2);
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS producto_nombre VARCHAR(100);
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS fecha_inicio TIMESTAMPTZ;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS fecha_fin TIMESTAMPTZ;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS horarios_validos JSONB;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS max_usos_por_usuario INT DEFAULT 1;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS max_usos_totales INT DEFAULT 0;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS usos_actuales INT DEFAULT 0;
