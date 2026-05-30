# Mapa, rubros y Charla con Piku (IA)

## Backend (Render)

Variables recomendadas (mismas que GestorNova):

- `GESTORNOVA_API_URL=https://api-gestornova.onrender.com` — proxy IA (opción recomendada)
- `GROQ_API_KEY` — fallback directo si GestorNova no responde
- `GROQ_MODEL=llama-3.3-70b-versatile`

Endpoints nuevos:

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/api/rubros` | Chips de filtros |
| GET | `/api/public/comercios?minLat&maxLat&minLon&maxLon` | Comercios en viewport |
| POST | `/api/chat-piku` | Asistente (requiere JWT cliente) |
| POST | `/api/usuario/eventos` | Registro silencioso de uso |

Tras deploy, las migraciones corren al iniciar el servidor (incluye `piku_eventos_usuario`).

El listado de comercios adapta el SQL si faltan columnas (`categoria`, `fecha_inicio`, etc.) en Neon antiguo.

## App Android

- Mapa: marcador azul del usuario, carga por área visible, chips de rubro, búsqueda por nombre.
- FAB 💬: chat «Charla con Piku» con sugerencia «Ver en mapa».
- Paleta: azul primario (`AzulPiku`).
