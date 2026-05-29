# SHA-1 en Windows (debug keystore)

`keytool` no está en el PATH por defecto. Usá el de Android Studio:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -list -v `
  -keystore "$env:USERPROFILE\.android\debug.keystore" `
  -alias androiddebugkey `
  -storepass android `
  -keypass android
```

Buscar la línea **SHA1:** (para Piku en este PC suele ser `A4:C6:C8:CD:AA:20:4B:F6:B2:32:FF:97:A7:16:13:FC:EB:0E:40:82`).

Alternativa desde el proyecto:

```powershell
cd C:\Users\leave\AndroidStudioProjects\Piku
.\gradlew :app:signingReport
```

## ¿Render usa el SHA-1?

**No.** En Render solo va `GOOGLE_CLIENT_ID` = ID del cliente OAuth **Aplicación web**.

El SHA-1 va **solo** en Google Cloud → credencial **Android** (paquete `com.piku.app`).
