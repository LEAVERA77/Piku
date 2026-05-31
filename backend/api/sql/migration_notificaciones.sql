-- Notificaciones persistentes + señal LISTEN/NOTIFY al canjear

CREATE TABLE IF NOT EXISTS piku_notificaciones (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comercio_id UUID NOT NULL REFERENCES piku_comercios(id) ON DELETE CASCADE,
    usuario_id UUID REFERENCES piku_usuarios(id) ON DELETE SET NULL,
    recompensa_id UUID REFERENCES piku_recompensas(id) ON DELETE SET NULL,
    canje_id UUID REFERENCES piku_canjes(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    titulo VARCHAR(255) NOT NULL,
    cuerpo TEXT NOT NULL,
    leida BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_piku_notificaciones_comercio
    ON piku_notificaciones(comercio_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_piku_notificaciones_no_leidas
    ON piku_notificaciones(comercio_id, created_at DESC)
    WHERE leida = false;

CREATE OR REPLACE FUNCTION piku_notify_canje()
RETURNS TRIGGER AS $$
DECLARE
    v_comercio_id UUID;
    v_nombre_oferta TEXT;
    v_notif_id UUID;
    v_payload TEXT;
BEGIN
    SELECT r.comercio_id, r.nombre
    INTO v_comercio_id, v_nombre_oferta
    FROM piku_recompensas r
    WHERE r.id = NEW.recompensa_id;

    IF v_comercio_id IS NULL THEN
        RETURN NEW;
    END IF;

    INSERT INTO piku_notificaciones (
        comercio_id, usuario_id, recompensa_id, canje_id, tipo, titulo, cuerpo
    ) VALUES (
        v_comercio_id,
        NEW.usuario_id,
        NEW.recompensa_id,
        NEW.id,
        'canje',
        'Nuevo canje',
        'Un cliente canjeó: ' || COALESCE(v_nombre_oferta, 'una oferta')
    )
    RETURNING id INTO v_notif_id;

    v_payload := json_build_object(
        'id', v_notif_id,
        'comercio_id', v_comercio_id,
        'tipo', 'canje'
    )::text;

    PERFORM pg_notify('piku_notificaciones', v_payload);

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_notificar_canje ON piku_canjes;
CREATE TRIGGER trigger_notificar_canje
    AFTER INSERT ON piku_canjes
    FOR EACH ROW
    EXECUTE FUNCTION piku_notify_canje();
