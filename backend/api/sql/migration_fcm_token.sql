ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS fcm_token TEXT;
CREATE INDEX IF NOT EXISTS idx_piku_usuarios_fcm ON piku_usuarios(comercio_id) WHERE fcm_token IS NOT NULL;
