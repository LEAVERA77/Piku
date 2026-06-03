package com.piku.app.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.piku.app.data.config.ConfigLoader
import kotlinx.coroutines.tasks.await

object GoogleSignInHelper {

    const val PACKAGE_NAME = "com.piku.app"
    private const val URL_CREDENCIALES =
        "https://console.cloud.google.com/apis/credentials?project=334957416226"

    fun webClientId(context: Context): String? =
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
                    "Falta el ID de cliente OAuth Web (config.json o default_web_client_id)"
                )
            )
        return Result.success(createClient(activity, webClientId).signInIntent)
    }

    /** Reutiliza la sesión de Google del dispositivo sin abrir UI (rápido si ya ingresó antes). */
    suspend fun silentIdToken(activity: Activity): String? {
        val webClientId = webClientId(activity) ?: return null
        return try {
            val account = createClient(activity, webClientId).silentSignIn().await()
            account.idToken?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    fun idTokenFromResult(context: Context, data: Intent?): Result<String> {
        return try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val token = account.idToken
                ?: return Result.failure(IllegalStateException("Google no devolvió idToken"))
            Result.success(token)
        } catch (e: ApiException) {
            Result.failure(IllegalStateException(mensajeParaApiException(context, e), e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mensajeErrorGoogle(context, e), e))
        }
    }

    fun mensajeErrorGoogle(context: Context, e: Throwable): String {
        val raw = e.message ?: e.toString()
        if (raw.contains("Developer console", ignoreCase = true) ||
            raw.contains("28444") ||
            raw.contains("code 10", ignoreCase = true) ||
            raw.contains("código 10", ignoreCase = true)
        ) {
            return mensajeConfiguracionCloud(context)
        }
        return raw
    }

    private fun mensajeParaApiException(context: Context, e: ApiException): String = when (e.statusCode) {
        10 -> mensajeConfiguracionCloud(context)
        12501 -> "Inicio de sesión cancelado"
        else -> "Error Google (${e.statusCode}): ${e.message}"
    }

    private fun mensajeConfiguracionCloud(context: Context): String {
        val sha1Dispositivo = AppSigningHelper.sha1Instalada(context) ?: "no detectado"
        val webId = webClientId(context)?.takeLast(24) ?: "no configurado"
        return """
            Google no reconoce esta instalación (código 10).

            En Google Cloud → Credenciales (proyecto 334957416226):
            1) Cliente OAuth tipo Android (no Web):
               paquete $PACKAGE_NAME
               SHA-1 de ESTE teléfono: $sha1Dispositivo
            2) Cliente OAuth tipo Aplicación web (en config): …$webId
               No uses el ID del cliente Android en la app.

            Si el SHA-1 no coincide con el que registraste, agregá este en la consola.
            Pantalla de consentimiento: agregá tu Gmail si está en modo Prueba.
            $URL_CREDENCIALES

            Después: esperá 10 min, desinstalá Piku e instalá de nuevo.
        """.trimIndent()
    }
}
