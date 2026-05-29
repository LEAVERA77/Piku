-- Login con Google: id externo y contraseña opcional
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS google_id VARCHAR(255) UNIQUE;
ALTER TABLE piku_usuarios ALTER COLUMN password_hash DROP NOT NULL;
CREATE INDEX IF NOT EXISTS idx_piku_usuarios_google_id ON piku_usuarios(google_id);
