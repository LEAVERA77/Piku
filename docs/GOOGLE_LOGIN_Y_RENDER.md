# Login con Google en el teléfono y variables en Render

## Cómo funciona el login (lo que pedís)

**No guardás la contraseña de Google en Render ni en la app.**

1. El usuario toca **«Continuar con Google»** en el celular.
2. Android muestra la cuenta de Google **ya guardada en el dispositivo** (la misma que usa Gmail, Play Store, etc.).
3. Google devuelve un **token seguro** (`idToken`) a la app.
4. La app envía ese token a tu backend: `POST /api/auth/google`.
5. Render verifica el token con Google usando `GOOGLE_CLIENT_ID` y crea la sesión (JWT).

Eso es «loguearse con la clave/cuenta de Google del teléfono». No hace falta escribir email ni contraseña de Google.

---

## Qué tenés que configurar (dos lugares, el mismo ID)

### 1) Google Cloud Console (una sola vez)

1. Entrá a https://console.cloud.google.com/
2. Creá un proyecto (o usá uno existente).
3. **APIs y servicios → Credenciales → Crear credenciales → ID de cliente de OAuth**
4. Tipo: **Aplicación web** → copiá el **ID de cliente** (termina en `.apps.googleusercontent.com`).
5. Para probar en Android, agregá también un **ID de cliente Android** con el **SHA-1** de tu keystore (Android Studio: Gradle → signingReport). El que va en Render y en `config.json` es el de **Web**, no el de Android.

### 2) En el teléfono (app)

Archivo **`app/src/main/assets/config.json`** (no se sube a Git; copiá de `config.example.json`):

```json
"google": {
  "webClientId": "EL_MISMO_ID_WEB.apps.googleusercontent.com"
}
```

### 3) En Render (backend)

En el servicio **piku-324e** (o el que uses) → **Environment**:

| Variable | ¿Obligatoria? | Qué poner |
|----------|---------------|-----------|
| `GOOGLE_CLIENT_ID` | **Sí**, si querés botón Google | El **mismo** ID Web de Google Cloud (idéntico a `webClientId` del `config.json`) |
| `DATABASE_URL` | Sí | URL de Neon |
| `JWT_SECRET` | Sí | Un secreto largo y aleatorio |
| `PIKU_CODIGO_INVITACION` | Recomendado | Código para registro de comercio (ej. `PIKU2025`, igual que en `config.json`). Sin esto, el alta de comercio puede fallar con «Invitación inválida». |
| `CLOUDINARY_CLOUD_NAME` | No | Solo si comercios suben **fotos de ofertas** |
| `CLOUDINARY_API_KEY` | No | Idem |
| `CLOUDINARY_API_SECRET` | No | Idem |

**No pongas** contraseña de Google, ni cuenta Gmail, ni SHA-1 en Render.

Si **no** configurás `GOOGLE_CLIENT_ID`, el login con email/contraseña y huella sigue funcionando; el botón de Google mostrará error de configuración.

---

## PIKU_CODIGO_INVITACION — ¿qué es?

Solo para **registrar un comercio** (dueño de local), no para clientes ni para Google.

- En la app: al crear cuenta → rol **Comerciante** → campo «Código de invitación».
- Si en Render tenés `PIKU_CODIGO_INVITACION=PIKU2025`, ese código es válido sin tener fila en la base de datos.
- Los clientes que entran con Google **no necesitan** este código.

---

## Cloudinary — ¿qué es?

Servicio para **guardar fotos de ofertas** que sube el comercio desde la app.

- Si no configurás Cloudinary, las ofertas se crean igual pero la subida de imagen desde el formulario puede fallar.
- Podés usar las mismas credenciales que GestorNova si ya las tenés.

---

## iPhone y otros sistemas

| Plataforma | Estado actual del repo |
|------------|------------------------|
| **Android** | App nativa en Kotlin (este proyecto) — Google Sign-In implementado |
| **iPhone (iOS)** | **No incluido** — haría falta una app iOS (Swift) o un proyecto multiplataforma (Flutter/React Native) |
| **Otros móviles** | Mismo caso que iOS |

La app **no es automáticamente** compatible con iPhone: el código actual es solo Android. El backend en Render sirve para una futura app iOS si usás el mismo `GOOGLE_CLIENT_ID` y `POST /api/auth/google`.

---

## ¿El ID cambia por usuario o por iPhone?

**No.** Ponés **un solo** `GOOGLE_CLIENT_ID` en Render (cliente OAuth **Web**). Todos los usuarios (María, Juan, iPhone, Android) usan la misma app; cada uno elige **su** cuenta Google en el teléfono.

Opcional en Render si usás clientes nativos distintos en Google Cloud:

- `GOOGLE_IOS_CLIENT_ID` — cliente iOS (Expo / iPhone)
- `GOOGLE_ANDROID_CLIENT_ID` — cliente Android (SHA-1)

El backend acepta tokens cuyo `aud` coincida con cualquiera de esos IDs configurados.

## iPhone

App Expo en `piku-mobile/` — ver `piku-mobile/README.md`. Misma API Render.

## Error «Developer console is not set up correctly» (28444) o código 10

La app ya usa el ID **Web** en `config.json`. Si el error persiste, falta el **cliente Android** en Google Cloud:

| Campo | Valor |
|--------|--------|
| Tipo de credencial | Android |
| Nombre del paquete | `com.piku.app` |
| SHA-1 (debug, este repo) | `A4:C6:C8:CD:AA:20:4B:F6:B2:32:FF:97:A7:16:13:FC:EB:0E:40:82` |

Comprobá que `google.webClientId` sea el ID del cliente **Aplicación web**, no el del cliente Android. Tras guardar en Google Cloud, esperá unos minutos, desinstalá la app e instalá de nuevo.

SHA-1 de release: `gradlew :app:signingReport` (variante release si firmás para Play Store).

Guía detallada con enlaces: **`docs/CREAR_CLIENTE_ANDROID_GOOGLE.md`**.

## Resumen rápido

- **Render `GOOGLE_CLIENT_ID`** = ID OAuth **Web** (fijo, no por usuario).
- **`config.json` → `google.webClientId`** = el **mismo** valor Web.
- **iPhone** → `piku-mobile` + opcional `GOOGLE_IOS_CLIENT_ID` en Render.
- El usuario inicia sesión con la **cuenta Google del teléfono**.
- **Cloudinary** y **PIKU_CODIGO_INVITACION** son opcionales.
