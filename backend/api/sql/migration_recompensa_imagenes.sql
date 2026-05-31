-- Galería de fotos por artículo (recompensa)
CREATE TABLE IF NOT EXISTS piku_recompensa_imagenes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recompensa_id UUID NOT NULL REFERENCES piku_recompensas(id) ON DELETE CASCADE,
    imagen_url TEXT NOT NULL,
    orden INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_piku_recompensa_imagenes_rec
    ON piku_recompensa_imagenes(recompensa_id, orden);
