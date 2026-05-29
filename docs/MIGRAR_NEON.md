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
```

## Opción B — Redeploy en Render

1. [Render Dashboard](https://dashboard.render.com) → servicio **Piku** → **Manual Deploy** → *Deploy latest commit*.
2. El servidor aplica migraciones al iniciar y el auth se adapta al esquema existente (commit `f7ae301`+).

## Render — variable recomendada

Para registro de **comercio** con código `PIKU2025`:

| Variable | Valor |
|----------|--------|
| `PIKU_CODIGO_INVITACION` | `PIKU2025` |

Ya tenés `GOOGLE_CLIENT_ID` correcto según tu captura.
