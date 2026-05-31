-- Alinear piku_reglas_puntos con la API actual (instalaciones con esquema viejo)
ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS puntos_por_peso NUMERIC(10, 2) NOT NULL DEFAULT 1;
ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS monto_minimo NUMERIC(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS puntos_fijos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS max_puntos_por_dia INTEGER NOT NULL DEFAULT 500;
ALTER TABLE piku_reglas_puntos ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'piku_reglas_puntos' AND column_name = 'puntos_por_unidad_moneda'
  ) THEN
    UPDATE piku_reglas_puntos
    SET puntos_por_peso = COALESCE(puntos_por_peso, puntos_por_unidad_moneda, 1)
    WHERE puntos_por_unidad_moneda IS NOT NULL;
  END IF;
END $$;
