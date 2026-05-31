# Integración con OpenStreetMap (Notes)

## Objetivo

Cuando un comercio se registra en Piku (`POST /api/auth/registro-comercio` o `POST /api/auth/registro-comercio-google`), el backend intenta crear un **Note** en OpenStreetMap en la ubicación del negocio. Eso aumenta la visibilidad del comercio en el ecosistema OSM y complementa la búsqueda por mapa en la app.

## Flujo

1. El comercio se guarda en `piku_comercios` (transacción habitual).
2. Si hay `lat` y `lon`, se llama a `backend/api/services/osm.service.js`.
3. Se hace `POST` a la API pública de OSM (no requiere OAuth para Notes).
4. Si la API devuelve el id del Note, se guarda opcionalmente en `osm_note_id` y `osm_note_created_at`.
5. Si OSM falla, solo se registra en logs: **el registro en Piku no se cancela**.

## Contenido del Note

Ejemplo de texto publicado:

```
📍 NUEVO COMERCIO registrado en PIKU

Nombre: Café Central
Categoría: cafeteria
Dirección: Av. Corrientes 1234

💚 App de fidelización por puntos
🔗 Más info: https://piku-324e.onrender.com/comercio/{uuid}
```

Los mappers y voluntarios de OSM pueden ver el Note en [openstreetmap.org](https://www.openstreetmap.org/) (capa Notes).

## Variables de entorno (Render)

| Variable | Obligatoria | Descripción |
|----------|-------------|-------------|
| `PIKU_PUBLIC_URL` | No | URL base para el enlace del comercio (default: `https://piku-324e.onrender.com`) |
| `OSM_API_BASE` | No | Base API (default: `https://api.openstreetmap.org/api/0.6`) |
| `OSM_USER_AGENT` | Recomendada | User-Agent identificable (política de uso de OSM) |

No se requieren credenciales OAuth para crear Notes.

## Archivos relevantes

- `backend/api/services/osm.service.js` — cliente HTTP y formato del texto
- `backend/api/controllers/auth.controller.js` — `publicarComercioEnOsm()` tras el registro
- `backend/api/sql/migration_comercio_osm_note.sql` — columnas opcionales en BD

## Referencias

- [API v0.6 — Notes](https://wiki.openstreetmap.org/wiki/API_v0.6#Notes)
- Ejemplo: `POST /api/0.6/notes?lat=...&lon=...&text=...`
