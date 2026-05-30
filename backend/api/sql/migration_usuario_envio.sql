-- Datos de envío a domicilio y perfil extendido
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS direccion_entrega TEXT;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS ciudad VARCHAR(100);
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS provincia VARCHAR(100);
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS codigo_postal VARCHAR(20);
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS notas_entrega TEXT;
