# Variables de entorno en Render (Piku)

## DATABASE_URL

- **Pooled** (`-pooler` en el host): consultas normales de la API.
- **DATABASE_URL_DIRECT** (sin `-pooler`): listener `LISTEN/NOTIFY`.

Pegá el valor **sin comillas** en Render. Si guardás `"postgresql://..."`, las comillas pueden entrar en la URL y fallar la conexión.

## GESTORNOVA_API_URL

Debe ser exactamente (sin `\n` ni comillas):

```
https://api-gestornova.onrender.com
```

Si en Render ves `https://api-gestornova.onrender.com\n`, editá y borrá el salto de línea. El backend ahora hace `trim` por si queda basura.

## Seed local

Render **no** reemplaza `backend/api/.env` en tu PC. Para `npm run seed:cerrito`, copiá `DATABASE_URL` de Render a un archivo `.env` local (ver `docs/SEED_CERRITO.md`).
