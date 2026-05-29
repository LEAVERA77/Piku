# Piku Mobile (iPhone + Android con Expo)

Cliente multiplataforma que usa el **mismo backend** en Render que la app Android nativa (`app/`).

## Login con Google en el teléfono

1. El usuario toca **Continuar con Google**.
2. iOS/Android muestra la **cuenta Google guardada en el dispositivo**.
3. La app envía `idToken` a `POST /api/auth/google`.

## Configuración (un solo ID para todos los usuarios)

| Dónde | Variable | Valor |
|-------|----------|--------|
| **Render** | `GOOGLE_CLIENT_ID` | ID OAuth **Web** de Google Cloud |
| **Android** | `config.json` → `google.webClientId` | **El mismo** ID |
| **Expo** | `app.config.ts` o `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID` | **El mismo** ID |

No cambia por usuario ni por iPhone/Android.

En Google Cloud, creá también credencial **iOS** con Bundle ID `com.piku.app`.

## Ejecutar en iPhone

```bash
cd piku-mobile
npm install
# Editar app.config.ts con tu webClientId
npx expo start
```

Escaneá el QR con la app **Expo Go** en el iPhone, o build con EAS en Mac para App Store.

## Ejecutar en Android (Expo)

```bash
npx expo start --android
```

La app Kotlin en `../app/` sigue siendo la versión completa (mapa, QR, panel comercio).
