package com.piku.app.utils

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.piku.app.data.config.ConfigLoader

object GoogleAuthHelper {

    suspend fun obtenerIdToken(context: Context): Result<String> {
        val webClientId = ConfigLoader.googleWebClientId(context)
            ?: return Result.failure(
                IllegalStateException(
                    "Configurá google.webClientId en assets/config.json (OAuth Web Client de Google Cloud)"
                )
            )

        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(option)
                .build()

            val result = CredentialManager.create(context).getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(googleCred.idToken)
            } else {
                Result.failure(IllegalStateException("Credencial de Google no reconocida"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
