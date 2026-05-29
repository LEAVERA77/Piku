# Arreglar error Google código 10 (Piku)

El **código 10** significa que Google no reconoce tu APK: falta la credencial **Android** en el **mismo proyecto** que el cliente Web.

## Render vs Google Cloud

| Dónde | Qué configurar |
|--------|----------------|
| **Render** | `GOOGLE_CLIENT_ID` = ID cliente **Aplicación web** (no SHA-1) |
| **Google Cloud** | Cliente **Android**: paquete + SHA-1 (no va en Render) |
| **Google Cloud** | Cliente **Web**: mismo ID que en Render |

Si el SHA-1 de `keytool` / `signingReport` coincide con el de la consola y el error 10 sigue, revisá que existan **dos** clientes (Web + Android) y tu Gmail en usuarios de prueba.

## Datos exactos de esta app

| Campo | Valor |
|--------|--------|
| Proyecto (número) | `334957416226` |
| Paquete | `com.piku.app` |
| SHA-1 debug | `A4:C6:C8:CD:AA:20:4B:F6:B2:32:FF:97:A7:16:13:FC:EB:0E:40:82` |
| ID cliente **Web** (config.json + Render) | `334957416226-0v6njnsr0sqrb1kfo5s2eisirm8lc0qu.apps.googleusercontent.com` |

Comprobar SHA-1 local: `gradlew :app:signingReport` (variante **debug**).

## Pasos en Google Cloud

1. Abrí [Credenciales del proyecto](https://console.cloud.google.com/apis/credentials?project=334957416226).
2. Si no existe, configurá la [Pantalla de consentimiento OAuth](https://console.cloud.google.com/apis/credentials/consent?project=334957416226) (tipo Externa o Interna; en Prueba, agregá tu Gmail como usuario de prueba).
3. **Crear credenciales** → **ID de cliente de OAuth**.
4. Tipo de aplicación: **Android**.
5. Nombre: `Piku Android debug` (o el que quieras).
6. Nombre del paquete: `com.piku.app`
7. Huella digital SHA-1: `A4:C6:C8:CD:AA:20:4B:F6:B2:32:FF:97:A7:16:13:FC:EB:0E:40:82`
8. Crear.

**No** uses el ID del cliente Android en `config.json`. Ahí va solo el ID del cliente **Aplicación web** (el de la tabla).

9. Verificá que ya exista un cliente **Aplicación web** con el ID de arriba.
10. En [Render](https://dashboard.render.com), variable `GOOGLE_CLIENT_ID` = mismo ID Web.
11. Esperá **5–10 minutos**, desinstalá Piku del teléfono/emulador e instalá de nuevo.

## Errores frecuentes

- Cliente Android creado en **otro** proyecto de Google Cloud.
- SHA-1 de otro PC (otro `debug.keystore`) — la app muestra **SHA-1 de ESTE teléfono** en el error; copiá ese valor en Google Cloud.
- Copiar el ID del cliente **Android** en `webClientId` (debe ser el **Web**).
- Pantalla de consentimiento sin publicar / usuario no agregado en modo Prueba.
- Necesitás **dos** credenciales: una **Android** (paquete + SHA-1) y una **Aplicación web** (ID en Render y en la app).

## Render

El backend solo valida el token después de que Google acepte el login en el teléfono. Si ves código 10 en la app, el problema está **antes** de llamar a la API.
