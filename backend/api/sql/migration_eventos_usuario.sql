-- Eventos de uso para personalización del asistente Piku
CREATE TABLE IF NOT EXISTS piku_eventos_usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID NOT NULL REFERENCES piku_usuarios(id) ON DELETE CASCADE,
    tipo_evento VARCHAR(40) NOT NULL CHECK (
        tipo_evento IN ('vista_comercio', 'canje', 'busqueda', 'mapa_abierto', 'chat_piku')
    ),
    comercio_id UUID REFERENCES piku_comercios(id) ON DELETE SET NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_piku_eventos_usuario_user ON piku_eventos_usuario(usuario_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_piku_eventos_usuario_tipo ON piku_eventos_usuario(tipo_evento);
