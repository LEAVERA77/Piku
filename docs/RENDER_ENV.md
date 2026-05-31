# Variables de entorno en Render (Piku)

## DATABASE_URL

- **Pooled** (`-pooler` en el host): consultas normales de la API.
- **DATABASE_URL_DIRECT** (sin `-pooler`): listener `LISTEN/NOTIFY`.

Pegá el valor **sin comillas** en Render. Si guardás `"postgresql://..."`, las comillas pueden entrar en la URL y fallar la conexión.

## Chat Piku (IA) — Groq, no GestorNova en el navegador

Si abrís `https://api-gestornova.onrender.com` en el navegador y ves **Not Found**, es normal: esa URL es una API sin página de inicio (GET `/` → 404).

**Piku no depende de abrir esa URL.** El chat usa:

1. **`GROQ_API_KEY`** (obligatorio para IA) → llama directo a Groq.
2. **`GESTORNOVA_API_URL`** (opcional) → solo si GestorNova tuviera rutas como `/api/ia/chat`. Hoy ese deploy suele responder 404 en esas rutas.

Con `GROQ_API_KEY` configurada en Render, **Chat Piku funciona** aunque GestorNova muestre Not Found.

Podés **eliminar** `GESTORNOVA_API_URL` en Render o dejarla; no afecta si Groq responde.

Para no intentar GestorNova nunca: `DISABLE_GESTORNOVA_IA=true`

### GESTORNOVA_API_URL (solo si usás proxy en el futuro)

Sin `\n` ni comillas:

```
https://api-gestornova.onrender.com
```

## Seed local

Render **no** reemplaza `backend/api/.env` en tu PC. Para `npm run seed:cerrito`, copiá `DATABASE_URL` de Render a un archivo `.env` local (ver `docs/SEED_CERRITO.md`).
