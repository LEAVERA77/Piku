package com.piku.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.data.repository.AuthRepository
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.PikuPhotoOverlay
import com.piku.app.ui.media.PikuImages
import com.piku.app.utils.BiometricHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginCliente: () -> Unit,
    onLoginComercio: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            PikuPhotoOverlay(
                url = PikuImages.heroLogin,
                modifier = Modifier.fillMaxSize(),
                overlayAlpha = 0.35f,
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom
            ) {
                Text(
                    "Piku",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    "Tus puntos, tus descuentos",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))

            BotonPiku(
                texto = if (cargando) "Ingresando…" else "Iniciar sesión",
                onClick = {
                    cargando = true
                    error = null
                    CoroutineScope(Dispatchers.Main).launch {
                        try {
                            val res = AuthRepository(context).login(email, password)
                            val esComercio = res.usuario.rol == "comercio"
                            if (activity != null && BiometricHelper.puedeUsarBiometrico(activity)) {
                                BiometricHelper.autenticar(
                                    activity = activity,
                                    titulo = "¿Guardar huella?",
                                    subtitulo = "Accedé más rápido la próxima vez",
                                    onExito = {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            AuthDataStore.setBiometricEnabled(context, true)
                                            if (esComercio) onLoginComercio() else onLoginCliente()
                                        }
                                    },
                                    onError = {
                                        if (esComercio) onLoginComercio() else onLoginCliente()
                                    }
                                )
                            } else {
                                if (esComercio) onLoginComercio() else onLoginCliente()
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Error al iniciar sesión"
                        } finally {
                            cargando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                habilitado = !cargando
            )

            if (activity != null && BiometricHelper.puedeUsarBiometrico(activity)) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.Main).launch {
                            if (!AuthDataStore.hasSession(context)) {
                                error = "Iniciá sesión una vez con email y contraseña"
                                return@launch
                            }
                            BiometricHelper.autenticar(
                                activity = activity,
                                onExito = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val rol = AuthDataStore.rol(context)
                                        if (rol == "comercio") onLoginComercio() else onLoginCliente()
                                    }
                                },
                                onError = { err -> error = err }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar huella digital")
                }
            }
        }
    }
}
