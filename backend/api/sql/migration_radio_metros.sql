-- Radio de validación QR (metros alrededor del comercio)
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS radio_metros INTEGER NOT NULL DEFAULT 100;
