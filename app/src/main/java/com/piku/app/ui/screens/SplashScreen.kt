package com.piku.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.piku.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.PikuTheme
import androidx.fragment.app.FragmentActivity
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.datastore.AppPreferences
import com.piku.app.data.datastore.AuthDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.piku.app.data.repository.AuthRepository
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.utils.BiometricHelper
import kotlinx.coroutines.launch

private enum class PasoSplash { CARGANDO, HUELLA, ERROR_HUELLA }

@Composable
fun SplashScreen(
    onIrElegirTipo: () -> Unit,
    onIrLogin: () -> Unit,
    onIrCliente: () -> Unit,
    onIrComercio: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val repo = remember { AuthRepository(context) }
    val tagline = ConfigLoader.appTagline(context)

    var paso by remember { mutableStateOf(PasoSplash.CARGANDO) }
    var rolGuardado by remember { mutableStateOf<String?>(null) }

    fun entrarSegunRol() {
        if (rolGuardado == "comercio") onIrComercio() else onIrCliente()
    }

    fun pedirHuella() {
        val act = activity
        if (act == null || !BiometricHelper.puedeUsarBiometrico(act)) {
            entrarSegunRol()
            return
        }
        paso = PasoSplash.HUELLA
        BiometricHelper.autenticar(
            activity = act,
            titulo = "Piku",
            subtitulo = "Usá tu huella para continuar",
            onExito = { entrarSegunRol() },
            onError = { paso = PasoSplash.ERROR_HUELLA }
        )
    }

    LaunchedEffect(Unit) {
        val hasSession = withContext(Dispatchers.IO) { AuthDataStore.hasSession(context) }
        if (!hasSession) {
            val elegirTipo = withContext(Dispatchers.IO) {
                AppPreferences.necesitaElegirTipoUsuario(context)
            }
            if (elegirTipo) onIrElegirTipo() else onIrLogin()
            return@LaunchedEffect
        }
        rolGuardado = withContext(Dispatchers.IO) { AuthDataStore.rol(context) }
        // Validamos el token en el servidor ANTES de entrar (máx. ~2,5 s, tolerante
        // a estar offline). Antes se validaba en paralelo y la corrutina se cancelaba
        // al navegar, así que un token vencido nunca expulsaba al login.
        val sesionValida = withContext(Dispatchers.IO) { repo.validarSesionRemota() }
        if (!sesionValida) {
            onIrLogin()
            return@LaunchedEffect
        }
        val biometrico = withContext(Dispatchers.IO) { AuthDataStore.isBiometricEnabled(context) }
        if (biometrico) {
            pedirHuella()
        } else {
            entrarSegunRol()
        }
    }

    val taglineColor = Color(0xFF455A64)
    val accentColor = NaranjaPiku

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.piku_logo_brand),
                contentDescription = "Piku",
                modifier = Modifier
                    .size(width = 220.dp, height = 200.dp)
                    .heightIn(max = 220.dp)
                    .padding(bottom = 8.dp),
                contentScale = ContentScale.Fit
            )
            if (tagline.isNotBlank()) {
                Text(
                    text = tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = taglineColor
                )
            }
            when (paso) {
                PasoSplash.CARGANDO -> {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 32.dp),
                        color = accentColor
                    )
                }
                PasoSplash.HUELLA -> {
                    Text(
                        "Confirmá con tu huella",
                        style = MaterialTheme.typography.bodyMedium,
                        color = taglineColor,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    TextButton(
                        onClick = {
                            scope.launch {
                                AuthDataStore.clear(context)
                                onIrLogin()
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Ingresar con email o Google", color = accentColor)
                    }
                }
                PasoSplash.ERROR_HUELLA -> {
                    Text(
                        "No se pudo verificar la huella",
                        style = MaterialTheme.typography.bodyMedium,
                        color = taglineColor,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { pedirHuella() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reintentar huella")
                    }
                    TextButton(
                        onClick = {
                            scope.launch {
                                AuthDataStore.setBiometricEnabled(context, false)
                                onIrLogin()
                            }
                        },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text("Usar email, Google o contraseña", color = accentColor)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSplashScreen() {
    PikuTheme {
        SplashScreen(
            onIrElegirTipo = {},
            onIrLogin = {},
            onIrCliente = {},
            onIrComercio = {}
        )
    }
}
