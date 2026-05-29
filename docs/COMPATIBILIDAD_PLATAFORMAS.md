# Compatibilidad de plataformas — Piku

## Hoy

| Plataforma | Carpeta | Funciones |
|------------|---------|-----------|
| **Android** | `app/` | App completa (mapa, QR, comercio, huella) |
| **iPhone / iOS** | `piku-mobile/` | Login Google + email, saldo (Expo; ampliar features) |
| **Backend** | `backend/api/` | API única en Render |

## Google OAuth

- Un solo `GOOGLE_CLIENT_ID` (Web) en Render para **todos** los usuarios.
- Opcional: `GOOGLE_IOS_CLIENT_ID` para tokens generados en iPhone.

## Regla Cursor

Ver `.cursor/rules/piku-google-oauth-multiplataforma.mdc` (siempre activa en este proyecto).
