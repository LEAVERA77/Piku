package com.piku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.data.model.LoginResponse
import com.piku.app.data.repository.AuthRepository
import com.piku.app.ui.components.BotonGoogle
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.components.PikuLogo
import com.piku.app.ui.theme.AmarilloPiku
import com.piku.app.ui.theme.CelestePiku
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.utils.BiometricHelper
import com.piku.app.utils.GoogleSignInHelper
import kotlinx.coroutines.launch

private enum class ModoAuth { INGRESAR, REGISTRAR }
private enum class RolRegistro { CLIENTE, COMERCIO }

@Composable
fun AuthScreen(
    onLoginCliente: () -> Unit,
    onLoginComercio: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    val tagline = remember { ConfigLoader.appTagline(context) }

    var modo by remember { mutableStateOf(ModoAuth.INGRESAR) }
    var nombre by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var rolRegistro by remember { mutableStateOf(RolRegistro.CLIENTE) }
    var nombreComercio by remember { mutableStateOf("") }
    var direccionComercio by remember { mutableStateOf("") }
    var categoriaComercio by remember { mutableStateOf("cafeteria") }
    var codigoInvitacion by remember {
        mutableStateOf(ConfigLoader.codigoInvitacionComercio(context).orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var puedeHuella by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        puedeHuella = AuthDataStore.hasSession(context) &&
            activity != null &&
            BiometricHelper.puedeUsarBiometrico(activity)
    }

    fun navegarTrasLogin(res: LoginResponse) {
        val esComercio = res.usuario.rol == "comercio"
        if (activity != null && BiometricHelper.puedeUsarBiometrico(activity)) {
            BiometricHelper.autenticar(
                activity = activity,
                titulo = "¿Activar huella digital?",
                subtitulo = "Entrá más rápido la próxima vez",
                onExito = {
                    scope.launch {
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
    }

    fun ejecutar(block: suspend () -> LoginResponse) {
        cargando = true
        error = null
        scope.launch {
            try {
                val res = block()
                navegarTrasLogin(res)
            } catch (e: Exception) {
                error = e.message ?: "Error de autenticación"
            } finally {
                cargando = false
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        GoogleSignInHelper.idTokenFromResult(result.data)
            .onSuccess { token ->
                ejecutar { AuthRepository(context).loginGoogle(token) }
            }
            .onFailure { e ->
                error = e.message
                cargando = false
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        VerdePiku.copy(alpha = 0.12f),
                        AmarilloPiku.copy(alpha = 0.08f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(VerdePiku, CelestePiku, NaranjaPiku.copy(alpha = 0.85f))
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(vertical = 28.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PikuLogo(
                    showTagline = false,
                    onGradient = true,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    tagline,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = modo == ModoAuth.INGRESAR,
                    onClick = { modo = ModoAuth.INGRESAR; error = null },
                    label = { Text("Ingresar") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VerdePiku.copy(alpha = 0.2f),
                        selectedLabelColor = VerdePiku
                    )
                )
                FilterChip(
                    selected = modo == ModoAuth.REGISTRAR,
                    onClick = { modo = ModoAuth.REGISTRAR; error = null },
                    label = { Text("Crear cuenta") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NaranjaPiku.copy(alpha = 0.2f),
                        selectedLabelColor = NaranjaPiku
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (modo == ModoAuth.REGISTRAR) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = rolRegistro == RolRegistro.CLIENTE,
                        onClick = { rolRegistro = RolRegistro.CLIENTE },
                        label = { Text("Cliente") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = rolRegistro == RolRegistro.COMERCIO,
                        onClick = { rolRegistro = RolRegistro.COMERCIO },
                        label = { Text("Comerciante") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = {
                        Text(
                            if (rolRegistro == RolRegistro.COMERCIO) "Tu nombre" else "Nombre completo"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (rolRegistro == RolRegistro.COMERCIO) {
                    OutlinedTextField(
                        value = nombreComercio,
                        onValueChange = { nombreComercio = it },
                        label = { Text("Nombre del comercio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = direccionComercio,
                        onValueChange = { direccionComercio = it },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = categoriaComercio,
                        onValueChange = { categoriaComercio = it },
                        label = { Text("Categoría (ej. cafeteria, restaurante)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = codigoInvitacion,
                        onValueChange = { codigoInvitacion = it },
                        label = { Text("Código de invitación") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (modo == ModoAuth.REGISTRAR) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmar,
                    onValueChange = { confirmar = it },
                    label = { Text("Confirmar contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            BotonPiku(
                texto = when {
                    cargando && modo == ModoAuth.REGISTRAR -> "Creando cuenta…"
                    cargando -> "Ingresando…"
                    modo == ModoAuth.REGISTRAR -> "Registrarme"
                    else -> "Iniciar sesión"
                },
                onClick = {
                    if (modo == ModoAuth.REGISTRAR) {
                        when {
                            nombre.isBlank() -> error = "Ingresá tu nombre"
                            email.isBlank() -> error = "Ingresá tu email"
                            password.length < 6 -> error = "La contraseña debe tener al menos 6 caracteres"
                            password != confirmar -> error = "Las contraseñas no coinciden"
                            rolRegistro == RolRegistro.COMERCIO && nombreComercio.isBlank() ->
                                error = "Ingresá el nombre del comercio"
                            rolRegistro == RolRegistro.COMERCIO && codigoInvitacion.isBlank() ->
                                error = "Ingresá el código de invitación del comercio"
                            else -> ejecutar {
                                if (rolRegistro == RolRegistro.COMERCIO) {
                                    AuthRepository(context).registroComercio(
                                        nombre = nombre,
                                        email = email,
                                        password = password,
                                        nombreComercio = nombreComercio,
                                        telefono = telefono,
                                        direccion = direccionComercio,
                                        categoria = categoriaComercio,
                                        codigoInvitacion = codigoInvitacion
                                    )
                                } else {
                                    AuthRepository(context).registro(nombre, email, password, telefono)
                                }
                            }
                        }
                    } else {
                        if (email.isBlank() || password.isBlank()) {
                            error = "Email y contraseña requeridos"
                        } else {
                            ejecutar { AuthRepository(context).login(email, password) }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                habilitado = !cargando,
                estilo = if (modo == ModoAuth.REGISTRAR) EstiloBotonPiku.PRIMARIO else EstiloBotonPiku.SECUNDARIO
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    "  o  ",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            BotonGoogle(
                texto = if (modo == ModoAuth.REGISTRAR) "Registrarse con Google" else "Continuar con Google",
                habilitado = !cargando &&
                    !(modo == ModoAuth.REGISTRAR && rolRegistro == RolRegistro.COMERCIO),
                onClick = {
                    if (modo == ModoAuth.REGISTRAR && rolRegistro == RolRegistro.COMERCIO) {
                        error = "El registro de comercio es con email y contraseña"
                        return@BotonGoogle
                    }
                    val act = activity
                    if (act == null) {
                        error = "No se puede abrir el inicio de sesión con Google"
                        return@BotonGoogle
                    }
                    GoogleSignInHelper.signInIntent(act)
                        .onSuccess { intent ->
                            cargando = true
                            error = null
                            googleSignInLauncher.launch(intent)
                        }
                        .onFailure { e ->
                            error = e.message
                        }
                }
            )

            if (puedeHuella && activity != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            BiometricHelper.autenticar(
                                activity = activity,
                                titulo = "Huella digital",
                                subtitulo = "Confirmá tu identidad",
                                onExito = {
                                    scope.launch {
                                        val rol = AuthDataStore.rol(context)
                                        if (rol == "comercio") onLoginComercio() else onLoginCliente()
                                    }
                                },
                                onError = { err -> error = err }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = null,
                        tint = VerdePiku
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ingresar con huella digital")
                }
            }

            if (modo == ModoAuth.INGRESAR) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "¿Primera vez en Piku? Elegí «Crear cuenta» arriba.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
