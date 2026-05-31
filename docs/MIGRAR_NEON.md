# Arreglar error 500 al registrarse (Neon desactualizado)

Si ves `column "puntos_saldo" does not exist` o similar, la base en Neon no tiene las columnas que usa la API.

## Opción A — Consola SQL de Neon (rápido)

En [Neon](https://console.neon.tech) → tu proyecto → **SQL Editor**, pegá y ejecutá:

```sql
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS puntos_saldo INTEGER NOT NULL DEFAULT 0;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS activo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS avatar_url TEXT;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS comercio_id UUID;
ALTER TABLE piku_usuarios ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS usuario_id UUID;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS suscripcion_activa BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS categoria VARCHAR(50);

ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;
ALTER TABLE piku_invitaciones_comercio ADD COLUMN IF NOT EXISTS usado BOOLEAN NOT NULL DEFAULT FALSE;

-- Registro con dirección, mapa y ofertas (ver migration_registro_mapa_ofertas.sql)
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS lat DOUBLE PRECISION;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS lon DOUBLE PRECISION;
ALTER TABLE piku_usuarios ADD COLUMN IF NOT EXISTS direccion TEXT;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS tipo_comercio VARCHAR(50);
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS icono_emoji VARCHAR(10);
ALTER TABLE piku_recompensas ADD COLUMN IF NOT EXISTS condiciones TEXT;
CREATE INDEX IF NOT EXISTS idx_piku_comercios_coords ON piku_comercios(lat, lon);

-- Envíos a domicilio (comercios)
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS realiza_envios BOOLEAN DEFAULT false;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS envio_gratis BOOLEAN DEFAULT false;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS costo_envio DECIMAL(10,2) DEFAULT 0;
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS envio_minimo_compra DECIMAL(10,2);
ALTER TABLE piku_comercios ADD COLUMN IF NOT EXISTS telefono_contacto VARCHAR(20);
CREATE INDEX IF NOT EXISTS idx_piku_comercios_envios ON piku_comercios(realiza_envios);
```

## Opción B — Redeploy en Render

1. [Render Dashboard](https://dashboard.render.com) → servicio **Piku** → **Manual Deploy** → *Deploy latest commit*.
2. El servidor aplica migraciones al iniciar y el auth se adapta al esquema existente (commit `f7ae301`+).

## Notificaciones (LISTEN/NOTIFY)

Tras deploy, el servidor aplica `migration_notificaciones.sql` (tabla `piku_notificaciones` + trigger al canjear).

En Render, para el listener en tiempo real:

| Variable | Valor |
|----------|--------|
| `DATABASE_URL` | Conexión **directa** a Neon (sin `-pooler` en el host) |
| `DATABASE_URL_DIRECT` | (opcional) Misma URL directa si usás pooler en `DATABASE_URL` para queries |
| `DISABLE_PG_LISTEN` | `true` solo si querés desactivar el listener |
| `NOTIFICACIONES_RETENCION_DIAS` | Días de retención (default `90`) |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Base64 JSON Firebase para FCM (opcional) |

Ver también `docs/NOTIFICACIONES_TIEMPO_REAL.md`.

## Render — variable recomendada

Para registro de **comercio** con código `PIKU2025`:

| Variable | Valor |
|----------|--------|
| `PIKU_CODIGO_INVITACION` | `PIKU2025` |

Ya tenés `GOOGLE_CLIENT_ID` correcto según tu captura.
