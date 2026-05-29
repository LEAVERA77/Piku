package com.piku.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.utils.BiometricHelper

@Composable
fun SplashScreen(
    onIrLogin: () -> Unit,
    onIrCliente: () -> Unit,
    onIrComercio: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    LaunchedEffect(Unit) {
        val tieneSesion = AuthDataStore.hasSession(context)
        if (!tieneSesion) {
            onIrLogin()
            return@LaunchedEffect
        }
        val biometrico = AuthDataStore.isBiometricEnabled(context)
        val rol = AuthDataStore.rol(context)
        if (biometrico && activity != null && BiometricHelper.puedeUsarBiometrico(activity)) {
            BiometricHelper.autenticar(
                activity = activity,
                onExito = {
                    if (rol == "comercio") onIrComercio() else onIrCliente()
                },
                onError = { onIrLogin() }
            )
        } else {
            if (rol == "comercio") onIrComercio() else onIrCliente()
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
        Text(
            "Piku",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
