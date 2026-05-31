-- Configuración de envíos a domicilio para comercios
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS realiza_envios BOOLEAN DEFAULT false;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS envio_gratis BOOLEAN DEFAULT false;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS costo_envio DECIMAL(10,2) DEFAULT 0;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS envio_minimo_compra DECIMAL(10,2);
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS telefono_contacto VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_piku_comercios_envios ON piku_comercios(realiza_envios);
