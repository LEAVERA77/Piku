# Compatibilidad de plataformas — Piku

## Hoy

- **Android 7+ (API 24+)**: app completa en `app/` (Kotlin + Jetpack Compose).
- **Backend (Render)**: API REST usable desde cualquier cliente futuro.

## iPhone / iPad

No hay proyecto iOS en este repositorio. Para App Store haría falta:

- App nativa Swift/SwiftUI, o
- Flutter / React Native compartiendo el mismo backend.

El login con Google en iOS usaría el SDK de Google para iOS y el **mismo** `GOOGLE_CLIENT_ID` (Web) en el backend.

## Recomendación

Mantener **un solo backend** en Render y, si necesitás iPhone, planificar una segunda app cliente que consuma las mismas rutas (`/api/auth/google`, `/api/auth/login`, etc.).
