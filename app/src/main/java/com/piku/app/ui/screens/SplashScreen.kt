package com.piku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.ui.components.PikuLogo
import com.piku.app.ui.theme.AmarilloPiku
import com.piku.app.ui.theme.CelestePiku
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.utils.BiometricHelper

@Composable
fun SplashScreen(
    onIrLogin: () -> Unit,
    onIrCliente: () -> Unit,
    onIrComercio: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val tagline = ConfigLoader.appTagline(context)

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
                titulo = "Piku",
                subtitulo = "Usá tu huella para continuar",
                onExito = {
                    if (rol == "comercio") onIrComercio() else onIrCliente()
                },
                onError = { onIrLogin() }
            )
        } else {
            if (rol == "comercio") onIrComercio() else onIrCliente()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(VerdePiku, CelestePiku, NaranjaPiku.copy(alpha = 0.9f), AmarilloPiku.copy(alpha = 0.7f))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            PikuLogo(showTagline = false, modifier = Modifier.padding(bottom = 8.dp))
            Text(
                tagline,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.95f)
            )
            CircularProgressIndicator(
                modifier = Modifier.padding(top = 32.dp),
                color = Color.White
            )
        }
    }
}
