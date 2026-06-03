-- Historial de puntos y canjes (requerido para saldo del cliente).
CREATE TABLE IF NOT EXISTS piku_transacciones_puntos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES piku_usuarios(id) ON DELETE CASCADE,
    comercio_id UUID REFERENCES piku_comercios(id) ON DELETE SET NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ganado', 'canjeado')),
    puntos INTEGER NOT NULL,
    descripcion TEXT,
    qr_codigo_id UUID,
    recompensa_id UUID,
    codigo_canje VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_piku_transacciones_usuario ON piku_transacciones_puntos(usuario_id, created_at DESC);

CREATE TABLE IF NOT EXISTS piku_canjes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES piku_usuarios(id) ON DELETE CASCADE,
    recompensa_id UUID NOT NULL,
    puntos_usados INTEGER NOT NULL CHECK (puntos_usados > 0),
    codigo_canje VARCHAR(64) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL DEFAULT 'confirmado',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

UPDATE piku_usuarios SET puntos_saldo = 0 WHERE puntos_saldo IS NULL OR puntos_saldo < 0;

ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS codigo_referido VARCHAR(12);

CREATE UNIQUE INDEX IF NOT EXISTS idx_piku_usuarios_codigo_referido
  ON piku_usuarios(codigo_referido) WHERE codigo_referido IS NOT NULL;

DO $$
BEGIN
  ALTER TABLE piku_usuarios ADD CONSTRAINT chk_piku_usuarios_puntos_nonneg CHECK (puntos_saldo >= 0);
EXCEPTION
  WHEN duplicate_object THEN NULL;
END $$;
