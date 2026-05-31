# Comercios de prueba — Cerrito, Entre Ríos

## Ejecutar el seed

### Opción A — Local (con `DATABASE_URL` en `backend/api/.env`)

```bash
cd backend/api
npm run seed:cerrito
```

### Opción B — Neon SQL Editor

No uses el `.sql` para inserts (el hash de contraseña debe ser bcrypt real). Ejecutá el script Node apuntando a Neon:

```bash
cd backend/api
set DATABASE_URL=postgresql://...@ep-xxx.neon.tech/neondb?sslmode=require
node scripts/seedComerciosCerrito.js
```

(PowerShell: `$env:DATABASE_URL="..."`)

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
