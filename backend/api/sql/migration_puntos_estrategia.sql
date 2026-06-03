-- Estrategia de puntos: bienvenida, cumpleaños, reglas por monto
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS bono_bienvenida_otorgado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS unidad_moneda NUMERIC(10, 2) NOT NULL DEFAULT 10;

COMMENT ON COLUMN piku_reglas_puntos.unidad_moneda IS 'Monto en pesos por lote de puntos (ej: 10 = 1 punto cada $10)';
