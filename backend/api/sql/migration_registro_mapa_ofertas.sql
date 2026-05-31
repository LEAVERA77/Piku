-- Registro con dirección, mapa con ofertas y detalle de recompensas
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS lat DOUBLE PRECISION;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS lon DOUBLE PRECISION;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS direccion TEXT;

ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS tipo_comercio VARCHAR(50);
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS icono_emoji VARCHAR(10);

ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS condiciones TEXT;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS vigencia_desde TIMESTAMPTZ;
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS vigencia_hasta TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_piku_comercios_coords ON piku_comercios(lat, lon);
