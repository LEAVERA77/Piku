-- Suscripciones y uso mensual por comercio

ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS plan VARCHAR(20) DEFAULT 'gratuito';

CREATE TABLE IF NOT EXISTS piku_uso_mensual (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comercio_id UUID NOT NULL REFERENCES piku_comercios(id) ON DELETE CASCADE,
    mes INTEGER NOT NULL,
    puntos_otorgados INT NOT NULL DEFAULT 0,
    notificaciones_enviadas INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(comercio_id, mes)
);

CREATE INDEX IF NOT EXISTS idx_piku_uso_mensual_comercio_mes
    ON piku_uso_mensual(comercio_id, mes);

CREATE TABLE IF NOT EXISTS piku_suscripciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comercio_id UUID NOT NULL REFERENCES piku_comercios(id) ON DELETE CASCADE,
    plan VARCHAR(20) NOT NULL DEFAULT 'gratuito',
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_inicio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    fecha_proximo_pago TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_piku_suscripciones_comercio
    ON piku_suscripciones(comercio_id);

INSERT INTO piku_suscripciones (comercio_id, plan, activa, fecha_inicio)
SELECT c.id, COALESCE(c.plan, 'gratuito'), TRUE, NOW()
FROM piku_comercios c
WHERE NOT EXISTS (
    SELECT 1 FROM piku_suscripciones s WHERE s.comercio_id = c.id
);
