package com.piku.app.utils

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.piku.app.data.config.ConfigLoader

object GoogleSignInHelper {

    /** SHA-1 debug de este proyecto (gradlew :app:signingReport). Registrar en Google Cloud → cliente Android. */
    const val SHA1_DEBUG_REFERENCIA = "A4:C6:C8:CD:AA:20:4B:F6:B2:32:FF:97:A7:16:13:FC:EB:0E:40:82"
    const val PACKAGE_NAME = "com.piku.app"

    fun webClientId(context: android.content.Context): String? =
        ConfigLoader.googleWebClientId(context)?.trim()?.takeIf { it.isNotBlank() }

    fun createClient(activity: Activity, webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(activity, gso)
    }

    fun signInIntent(activity: Activity): Result<Intent> {
        val webClientId = webClientId(activity)
            ?: return Result.failure(
                IllegalStateException(
                    "Falta google.webClientId en assets/config.json"
                )
            )
        return Result.success(createClient(activity, webClientId).signInIntent)
    }

    fun idTokenFromResult(data: Intent?): Result<String> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val token = account.idToken
                ?: return Result.failure(IllegalStateException("Google no devolvió idToken"))
            Result.success(token)
        } catch (e: ApiException) {
            Result.failure(IllegalStateException(mensajeParaApiException(e), e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mensajeConsolaGoogle(e), e))
        }
    }

    private const val URL_CREDENCIALES =
        "https://console.cloud.google.com/apis/credentials?project=334957416226"

    private fun mensajeParaApiException(e: ApiException): String = when (e.statusCode) {
        10 -> """
            Google Cloud no reconoce esta app (código 10).
            Creá un cliente OAuth tipo Android en el proyecto 334957416226:
            paquete $PACKAGE_NAME, SHA-1 $SHA1_DEBUG_REFERENCIA.
            Guía: docs/CREAR_CLIENTE_ANDROID_GOOGLE.md
            Consola: $URL_CREDENCIALES
            Luego esperá 5–10 min, desinstalá e instalá Piku.
        """.trimIndent()
        12501 -> "Inicio de sesión cancelado"
        else -> "Error Google (${e.statusCode}): ${e.message}"
    }

    fun mensajeConsolaGoogle(e: Throwable): String {
        val raw = e.message ?: e.toString()
        return if (raw.contains("Developer console", ignoreCase = true) ||
            raw.contains("28444")
        ) {
            """
                Consola de desarrollador de Google mal configurada.
                Cliente Android: paquete $PACKAGE_NAME, SHA-1 $SHA1_DEBUG_REFERENCIA.
                config.json → google.webClientId = ID de cliente tipo «Aplicación web» (mismo que GOOGLE_CLIENT_ID en Render).
            """.trimIndent()
        } else {
            raw
        }
    }
}
