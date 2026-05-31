# Notificaciones en tiempo real (Piku)

## Render — variables de entorno

| Variable | Uso |
|----------|-----|
| `DATABASE_URL` | Pool de consultas HTTP (puede usar pooler) |
| `DATABASE_URL_DIRECT` | **Recomendado:** host Neon **sin** `-pooler` solo para LISTEN/NOTIFY |
| `DISABLE_PG_LISTEN` | `true` desactiva el listener |
| `NOTIFICACIONES_RETENCION_DIAS` | Días antes de borrar filas (default `90`) |
| `DISABLE_NOTIFICACIONES_RETENCION` | `true` desactiva el job diario |
| `RATE_LIMIT_NOTIFICACIONES_MAX` | Máx. req/min en rutas de notificaciones (default `60`) |
| `RATE_LIMIT_CANJES_MAX` | Máx. req/min en historial canjes (default `40`) |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | Base64 del JSON de cuenta de servicio Firebase (FCM) |
| `FIREBASE_SERVICE_ACCOUNT` | Alternativa: JSON en texto (solo entornos seguros) |

Si `DATABASE_URL` contiene `-pooler` y no definís `DATABASE_URL_DIRECT`, el servidor intenta quitar `-pooler` del host y muestra un warning.

## WebSocket (app comercio abierta)

- URL: `wss://TU_API/ws/comercio?token=JWT`
- Solo rol `comercio` con `comercio_id`.
- Al canjear, el panel actualiza el badge de notificaciones sin refrescar manualmente.

## FCM (app en segundo plano)

1. Crear proyecto en [Firebase Console](https://console.firebase.google.com).
2. Agregar app Android `com.piku.app` y descargar `google-services.json` en `app/`.
3. En Render, pegar el JSON de cuenta de servicio en `FIREBASE_SERVICE_ACCOUNT_JSON` (base64).
4. En la app, registrar token con `PUT /api/comercio/dispositivo/fcm` body `{ "token": "..." }`.

La dependencia `firebase-admin` en el backend solo envía push si esas variables están configuradas.

## Seguridad

- Rate limit por usuario en notificaciones y canjes.
- Payload NOTIFY no se loguea en producción.
- Retención automática borra notificaciones antiguas (> 90 días por defecto).
