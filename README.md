# Piku

App de fidelización por puntos y descuentos basada en QR.

## Funcionalidades principales

- Registro e inicio de sesión (cliente y comercio, email o Google)
- Mapa de comercios con ubicación, rubros y búsqueda (incluye POIs de OpenStreetMap)
- Chat con asistente Piku (Groq)
- Perfil con datos de envío y avatar
- Panel de comercio: ofertas, QR y estadísticas

## Plus: visibilidad en OpenStreetMap

Al **registrar un comercio**, el backend crea automáticamente un **Note en OSM** con nombre, categoría, dirección y enlace a Piku. Si la API de OSM no responde, el registro en Piku sigue completándose con normalidad.

Detalle técnico: [docs/OSM_INTEGRATION.md](docs/OSM_INTEGRATION.md)

## Backend

- Node.js + Express en `backend/api/`
- Base de datos: PostgreSQL (Neon)
- Despliegue: Render (`https://piku-324e.onrender.com`)

## App Android

- Kotlin + Jetpack Compose en `app/`
- Mapa: osmdroid + Nominatim

## Documentación

- [Migración Neon](docs/MIGRAR_NEON.md)
- [Integración OSM](docs/OSM_INTEGRATION.md)
