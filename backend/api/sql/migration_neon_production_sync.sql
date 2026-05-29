-- Sincroniza BD Neon/producción con la API actual (ejecutar en consola SQL o al iniciar el servidor).
-- Idempotente: se puede correr varias veces.

-- Usuarios
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS telefono VARCHAR(50);
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS puntos_saldo INTEGER NOT NULL DEFAULT 0;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS comercio_id UUID;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE piku_usuarios ALTER COLUMN password_hash DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_piku_usuarios_google_id ON piku_usuarios(google_id) WHERE google_id IS NOT NULL;

-- Comercios (la API usa usuario_id y suscripcion_activa)
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS usuario_id UUID;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS suscripcion_activa BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS categoria VARCHAR(50);
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS logo_url TEXT;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS lat DOUBLE PRECISION;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS lon DOUBLE PRECISION;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

UPDATE piku_comercios
SET usuario_id = owner_usuario_id
WHERE usuario_id IS NULL
  AND owner_usuario_id IS NOT NULL;

-- Invitaciones comercio
ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS usado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS comercio_id UUID;
ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

-- Reglas de puntos (si falta la tabla en instalaciones viejas)
CREATE TABLE IF NOT EXISTS piku_reglas_puntos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comercio_id UUID NOT NULL UNIQUE,
    puntos_por_peso NUMERIC(10, 2) NOT NULL DEFAULT 1,
    monto_minimo NUMERIC(10, 2) NOT NULL DEFAULT 0,
    puntos_fijos INTEGER NOT NULL DEFAULT 0,
    max_puntos_por_dia INTEGER NOT NULL DEFAULT 500,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
