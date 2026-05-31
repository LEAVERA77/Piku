# Comercios de prueba — Cerrito, Entre Ríos

## Ejecutar el seed

### Opción A — Local (recomendado)

1. En [Neon Console](https://console.neon.tech) → tu proyecto → **Connect** → copiá la URL **PostgreSQL** (host tipo `ep-nombre-real-12345678.sa-east-1.aws.neon.tech`, **no** `ep-tu-proyecto`).

2. Creá el archivo `backend/api/.env` (no se sube a Git):

```env
DATABASE_URL=postgresql://neondb_owner:TU_PASSWORD@ep-XXXXX.region.aws.neon.tech/neondb?sslmode=require
```

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
