package com.piku.app.utils

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.piku.app.data.config.ConfigLoader

object GoogleAuthHelper {

    suspend fun obtenerIdToken(activity: ComponentActivity): Result<String> {
        val webClientId = ConfigLoader.googleWebClientId(activity)
            ?: return Result.failure(
                IllegalStateException(
                    "Falta el ID de cliente OAuth Web (config.json o default_web_client_id)"
                )
            )

        obtenerConCredentialManager(activity, webClientId, soloCuentasAutorizadas = true)
            .fold(onSuccess = { return Result.success(it) }, onFailure = { })

        obtenerConCredentialManager(activity, webClientId, soloCuentasAutorizadas = false)
            .fold(onSuccess = { return Result.success(it) }, onFailure = { err ->
                return Result.failure(err)
            })
    }

    private suspend fun obtenerConCredentialManager(
        activity: ComponentActivity,
        webClientId: String,
        soloCuentasAutorizadas: Boolean
    ): Result<String> {
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(soloCuentasAutorizadas)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(soloCuentasAutorizadas)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val result = CredentialManager.create(activity).getCredential(
                request = request,
                context = activity
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                val token = googleCred.idToken
                    ?: return Result.failure(IllegalStateException("Google no devolvió idToken"))
                Result.success(token)
            } else {
                Result.failure(IllegalStateException("Credencial de Google no reconocida"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(IllegalStateException("Inicio de sesión cancelado"))
        } catch (e: GetCredentialException) {
            Result.failure(IllegalStateException(GoogleSignInHelper.mensajeErrorGoogle(activity, e), e))
        } catch (e: Exception) {
            Result.failure(IllegalStateException(GoogleSignInHelper.mensajeErrorGoogle(activity, e), e))
        }
    }
}
