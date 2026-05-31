# Comercios de prueba — Cerrito, Entre Ríos

## Ejecutar el seed

### Opción A — Local (recomendado)

**Render y tu PC son distintos:** pegar `DATABASE_URL` en Render alcanza para la API en la nube. Para `npm run seed:cerrito` tenés que crear **`backend/api/.env`** en tu computadora (no aparece en Git; por eso no lo ves en el repo clonado).

1. En el Explorador, carpeta `Piku\backend\api` → copiá `.env.example` → pegá como **`.env`** (mismo nivel que `package.json`).

2. En [Neon Console](https://console.neon.tech) → **Connect** → copiá la URL **PostgreSQL** (la misma que pegaste en Render). Host real: `ep-algo-12345....neon.tech`, **no** `ep-tu-proyecto`.

3. Editá `.env`. Pegá **la misma** `DATABASE_URL` que tenés en Render (sin comillas alrededor):

```env
DATABASE_URL=postgresql://neondb_owner:TU_PASSWORD@ep-gentle-silence-adns9whd-pooler.c-2.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
```

**Sin** comillas `"` al inicio/fin de la línea. El pooler (`-pooler`) sirve para el seed.

También podés copiar `DATABASE_URL` desde **Render** → servicio Piku → **Environment** (el valor que ya usa la API en producción).

3. Ejecutá:

```powershell
cd C:\Users\leave\AndroidStudioProjects\Piku\backend\api
npm run seed:cerrito
```

Si ves `ENOTFOUND ep-tu-proyecto.neon.tech`, la URL sigue siendo el **ejemplo** de la documentación: reemplazala por la de Neon/Render.

### Opción B — Solo esta sesión (sin .env)

```powershell
cd backend\api
$env:DATABASE_URL="postgresql://...URL REAL COMPLETA..."
npm run seed:cerrito
```

---

## Credenciales (todas con contraseña `comercio123`)

| Comercio | Email |
|----------|--------|
| Café Martínez | cafe@martinez.com |
| Pizzería Don Juan | donjuan@pizzeria.com |
| Farmacia Cerrito | farmacia@cerrito.com |
| Moda Urbana | moda@urbana.com |
| Supermercado El Ahorro | ahorro@super.com |

Teléfono común: **3434540250**

---

## Probar en la app

1. Login **Comercio** con cualquier email de la tabla.
2. Mapa (cliente): centrá en Cerrito (~**-31.9189, -60.6085**) — deben verse los 5 pines con emoji.
3. Cada comercio tiene ofertas activas y envíos configurados.

El script es **idempotente**: si el email ya existe, no duplica usuario; solo agrega ofertas que falten.
