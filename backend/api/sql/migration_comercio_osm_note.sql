-- Referencia opcional al Note creado en OpenStreetMap al registrar el comercio
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS osm_note_id BIGINT;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS osm_note_created_at TIMESTAMPTZ;
