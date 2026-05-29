package com.piku.app.utils

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    fun puedeUsarBiometrico(activity: FragmentActivity): Boolean {
        val resultado = BiometricManager.from(activity)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return resultado == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun autenticar(
        activity: FragmentActivity,
        titulo: String = "Acceso a Piku",
        subtitulo: String = "Usá tu huella para continuar",
        onExito: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onExito()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onError("Huella no reconocida")
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(titulo)
            .setSubtitle(subtitulo)
            .setNegativeButtonText("Usar contraseña")
            .build()

        prompt.authenticate(info)
    }
}
