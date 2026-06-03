package com.piku.app.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.piku.app.R
import com.piku.app.ui.theme.PikuTheme
import androidx.fragment.app.FragmentActivity
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.datastore.AppPreferences
import com.piku.app.data.datastore.AuthDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.piku.app.data.model.LoginResponse
import com.piku.app.data.repository.AuthRepository
import com.piku.app.data.TipoComercio
import com.piku.app.ui.components.BotonGoogle
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.CampoContrasena
import com.piku.app.ui.components.DireccionFormState
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.components.FormularioDireccionRegistro
import com.piku.app.ui.components.SelectorUbicacionMapa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import com.piku.app.utils.BiometricHelper
import com.piku.app.utils.TelefonoUtil
import com.piku.app.utils.GoogleSignInHelper
import kotlinx.coroutines.launch

private enum class ModoAuth { INGRESAR, REGISTRAR }
private enum class RolRegistro { CLIENTE, COMERCIO }

private const val MIN_CARACTERES_NOMBRE = 2

private fun esNombreApellidoValido(texto: String): Boolean {
    val t = texto.trim()
    if (t.length < MIN_CARACTERES_NOMBRE) return false
    val letras = t.count { it.isLetter() }
    return letras >= MIN_CARACTERES_NOMBRE
}

private fun validarNombreApellido(nombre: String, apellido: String): String? {
    if (!esNombreApellidoValido(nombre)) {
        return "El nombre debe tener al menos $MIN_CARACTERES_NOMBRE letras"
    }
    if (!esNombreApellidoValido(apellido)) {
        return "El apellido debe tener al menos $MIN_CARACTERES_NOMBRE letras"
    }
    return null
}

private fun nombreCompleto(nombre: String, apellido: String): String =
    "${nombre.trim()} ${apellido.trim()}"

private fun validarTelefonoRegistro(telefono: String, esComercio: Boolean): String? {
    if (!esComercio) return null
    return TelefonoUtil.validarComercio(telefono)
}

private fun validarDireccion(form: DireccionFormState): String? {
    if (form.calle.trim().length < 2) return "Ingresá la calle"
    if (form.numero.trim().isBlank()) return "Ingresá el número"
    if (form.ciudad.trim().length < 2) return "Ingresá la ciudad"
    if (form.provincia.isBlank()) return "Seleccioná la provincia"
    return null
}

private fun validarComercioParaGoogle(nombreComercio: String, codigoInvitacion: String): String? {
    if (nombreComercio.trim().length < 2) return "Ingresá el nombre del comercio (mínimo 2 caracteres)"
    if (codigoInvitacion.isBlank()) return "Ingresá el código de invitación"
    return null
}

private fun nombreResponsableComercio(nombre: String, apellido: String): String? {
    val completo = nombreCompleto(nombre, apellido).trim()
    return completo.ifBlank { null }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onLoginCliente: () -> Unit,
    onLoginComercio: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val scope = rememberCoroutineScope()
    var tagline by remember { mutableStateOf("Tus puntos, tus descuentos") }
    val fieldShape = RoundedCornerShape(14.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        focusedLabelColor = MaterialTheme.colorScheme.primary
    )

    var modo by remember { mutableStateOf(ModoAuth.INGRESAR) }
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var rolRegistro by remember { mutableStateOf(RolRegistro.CLIENTE) }
    var nombreComercio by remember { mutableStateOf("") }
    var direccionForm by remember { mutableStateOf(DireccionFormState()) }
    var tipoComercioId by remember { mutableStateOf(TipoComercio.CAFETERIA.id) }
    var mapLat by remember { mutableStateOf(-34.6037) }
    var mapLon by remember { mutableStateOf(-58.3816) }
    var tipoComercioExpanded by remember { mutableStateOf(false) }
    var codigoInvitacion by remember {
        mutableStateOf(ConfigLoader.codigoInvitacionComercio(context).orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var puedeHuella by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val hasSession = AuthDataStore.hasSession(context)
            val biometric = AuthDataStore.isBiometricEnabled(context)
            val rolPref = AppPreferences.rolPreferidoInicio(context)
            tagline = ConfigLoader.appTagline(context)
            withContext(Dispatchers.Main) {
                puedeHuella = hasSession &&
                    biometric &&
                    activity != null &&
                    BiometricHelper.puedeUsarBiometrico(activity)
                if (rolPref == AppPreferences.ROL_COMERCIO) {
                    rolRegistro = RolRegistro.COMERCIO
                } else if (rolPref == AppPreferences.ROL_CLIENTE) {
                    rolRegistro = RolRegistro.CLIENTE
                }
            }
        }
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
                val res = withContext(Dispatchers.IO) { block() }
                navegarTrasLogin(res)
            } catch (e: Exception) {
                error = e.message ?: "Error de autenticación"
            } finally {
                cargando = false
            }
        }
    }

    fun ejecutarGoogle(idToken: String) {
        if (modo == ModoAuth.REGISTRAR && rolRegistro == RolRegistro.COMERCIO) {
            validarComercioParaGoogle(nombreComercio, codigoInvitacion)?.let {
                error = it
                return
            }
            validarDireccion(direccionForm)?.let {
                error = it
                return
            }
            validarTelefonoRegistro(telefono, esComercio = true)?.let {
                error = it
                return
            }
        }
        ejecutar {
            val repo = AuthRepository(context)
            if (modo == ModoAuth.REGISTRAR && rolRegistro == RolRegistro.COMERCIO) {
                val dir = direccionForm
                repo.registroComercioGoogle(
                    idToken = idToken,
                    nombre = nombreResponsableComercio(nombre, apellido),
                    nombreComercio = nombreComercio,
                    telefono = telefono,
                    calle = dir.calle,
                    numero = dir.numero,
                    ciudad = dir.ciudad,
                    provincia = dir.provincia,
                    codigoPostal = dir.codigoPostal,
                    lat = mapLat,
                    lon = mapLon,
                    tipoComercio = tipoComercioId,
                    codigoInvitacion = codigoInvitacion
                )
            } else {
                repo.loginGoogle(idToken)
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            cargando = false
            return@rememberLauncherForActivityResult
        }
        GoogleSignInHelper.idTokenFromResult(context, result.data)
            .onSuccess { token -> ejecutarGoogle(token) }
            .onFailure { e ->
                error = e.message
                cargando = false
            }
    }

    fun abrirSelectorGoogle() {
        val act = activity
        if (act == null) {
            error = "No se puede abrir el inicio de sesión con Google"
            return
        }
        cargando = true
        error = null
        GoogleSignInHelper.signInIntent(act)
            .onSuccess { intent -> googleSignInLauncher.launch(intent) }
            .onFailure { e ->
                error = e.message
                cargando = false
            }
    }

    fun iniciarGoogle() {
        val act = activity
        if (act == null) {
            error = "No se puede abrir el inicio de sesión con Google"
            return
        }
        if (modo == ModoAuth.REGISTRAR && rolRegistro == RolRegistro.COMERCIO) {
            validarComercioParaGoogle(nombreComercio, codigoInvitacion)?.let {
                error = it
                return
            }
            abrirSelectorGoogle()
            return
        }
        scope.launch {
            cargando = true
            error = null
            val token = withContext(Dispatchers.IO) {
                GoogleSignInHelper.silentIdToken(act)
            }
            if (!token.isNullOrBlank()) {
                ejecutarGoogle(token)
                return@launch
            }
            abrirSelectorGoogle()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.88f)
                        )
                    ),
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                )
                .padding(vertical = 28.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.piku_logo_brand),
                    contentDescription = "Piku",
                    modifier = Modifier
                        .size(width = 180.dp, height = 72.dp)
                        .padding(bottom = 4.dp),
                    contentScale = ContentScale.Fit
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
                .imePadding()
                .navigationBarsPadding()
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
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = modo == ModoAuth.REGISTRAR,
                    onClick = { modo = ModoAuth.REGISTRAR; error = null },
                    label = { Text("Crear cuenta") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = apellido,
                        onValueChange = { apellido = it },
                        label = { Text("Apellido") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (rolRegistro == RolRegistro.COMERCIO) {
                    OutlinedTextField(
                        value = nombreComercio,
                        onValueChange = { nombreComercio = it },
                        label = { Text("Nombre del comercio") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = codigoInvitacion,
                        onValueChange = { codigoInvitacion = it },
                        label = { Text("Código de invitación") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = fieldShape,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                BotonGoogle(
                    texto = when {
                        cargando && rolRegistro == RolRegistro.COMERCIO -> "Conectando con Google…"
                        rolRegistro == RolRegistro.COMERCIO -> "Registrar comercio con Google"
                        else -> "Registrarse con Google"
                    },
                    habilitado = !cargando,
                    onClick = { iniciarGoogle() }
                )
                error?.let { mensaje ->
                    Text(
                        mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    if (rolRegistro == RolRegistro.COMERCIO) {
                        "Completá nombre del comercio y código. Tu nombre puede venir de Google."
                    } else {
                        "Registro rápido con tu cuenta de Google."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        "  o con email  ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))

            }

            if (modo == ModoAuth.REGISTRAR) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Dirección",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                FormularioDireccionRegistro(
                    state = direccionForm,
                    onChange = { direccionForm = it },
                    fieldColors = fieldColors,
                    fieldShape = fieldShape
                )
                if (rolRegistro == RolRegistro.COMERCIO) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val tipoSeleccionado = TipoComercio.desdeId(tipoComercioId)
                    ExposedDropdownMenuBox(
                        expanded = tipoComercioExpanded,
                        onExpandedChange = { tipoComercioExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "${tipoSeleccionado.emoji} ${tipoSeleccionado.etiqueta}",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de comercio") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = tipoComercioExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = fieldShape,
                            colors = fieldColors
                        )
                        ExposedDropdownMenu(
                            expanded = tipoComercioExpanded,
                            onDismissRequest = { tipoComercioExpanded = false }
                        ) {
                            TipoComercio.entries.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text("${tipo.emoji} ${tipo.etiqueta}") },
                                    onClick = {
                                        tipoComercioId = tipo.id
                                        tipoComercioExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Ubicación en el mapa",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SelectorUbicacionMapa(
                        lat = mapLat,
                        lon = mapLon,
                        onCentroCambiado = { la, lo ->
                            mapLat = la
                            mapLon = lo
                        }
                    )
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = fieldShape,
                colors = fieldColors
            )
            Spacer(modifier = Modifier.height(10.dp))

            CampoContrasena(
                value = password,
                onValueChange = { password = it },
                label = "Contraseña",
                modifier = Modifier.fillMaxWidth()
            )

            if (modo == ModoAuth.REGISTRAR) {
                Spacer(modifier = Modifier.height(10.dp))
                CampoContrasena(
                    value = confirmar,
                    onValueChange = { confirmar = it },
                    label = "Confirmar contraseña",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it.filter { c -> c.isDigit() || c == '+' || c == ' ' || c == '-' } },
                    label = {
                        Text(
                            if (rolRegistro == RolRegistro.COMERCIO) {
                                "Teléfono * (ej: 3434567890)"
                            } else {
                                "Teléfono (opcional)"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = fieldShape,
                    colors = fieldColors
                )
            }

            if (modo != ModoAuth.REGISTRAR) {
                error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
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
                            validarNombreApellido(nombre, apellido) != null -> {
                                error = validarNombreApellido(nombre, apellido)
                            }
                            email.isBlank() -> error = "Ingresá tu email"
                            password.length < 6 -> error = "La contraseña debe tener al menos 6 caracteres"
                            password != confirmar -> error = "Las contraseñas no coinciden"
                            rolRegistro == RolRegistro.COMERCIO && nombreComercio.trim().length < 2 ->
                                error = "El nombre del comercio es demasiado corto"
                            rolRegistro == RolRegistro.COMERCIO && codigoInvitacion.isBlank() ->
                                error = "Ingresá el código de invitación del comercio"
                            validarDireccion(direccionForm) != null ->
                                error = validarDireccion(direccionForm)
                            validarTelefonoRegistro(telefono, rolRegistro == RolRegistro.COMERCIO) != null ->
                                error = validarTelefonoRegistro(telefono, rolRegistro == RolRegistro.COMERCIO)
                            else -> ejecutar {
                                val nombreEnvio = nombreCompleto(nombre, apellido)
                                val dir = direccionForm
                                if (rolRegistro == RolRegistro.COMERCIO) {
                                    AuthRepository(context).registroComercio(
                                        nombre = nombreEnvio,
                                        email = email,
                                        password = password,
                                        nombreComercio = nombreComercio,
                                        telefono = telefono,
                                        calle = dir.calle,
                                        numero = dir.numero,
                                        ciudad = dir.ciudad,
                                        provincia = dir.provincia,
                                        codigoPostal = dir.codigoPostal,
                                        lat = mapLat,
                                        lon = mapLon,
                                        tipoComercio = tipoComercioId,
                                        codigoInvitacion = codigoInvitacion
                                    )
                                } else {
                                    AuthRepository(context).registro(
                                        nombre = nombreEnvio,
                                        email = email,
                                        password = password,
                                        telefono = telefono,
                                        calle = dir.calle,
                                        numero = dir.numero,
                                        ciudad = dir.ciudad,
                                        provincia = dir.provincia,
                                        codigoPostal = dir.codigoPostal
                                    )
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

            if (modo == ModoAuth.INGRESAR) {
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
                    texto = "Continuar con Google",
                    habilitado = !cargando,
                    onClick = { iniciarGoogle() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (puedeHuella && activity != null) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val repo = AuthRepository(context)
                            if (!withContext(Dispatchers.IO) { repo.validarSesionRemota() }) {
                                error = "Sesión expirada. Ingresá de nuevo con email o Google."
                                puedeHuella = false
                                return@launch
                            }
                            BiometricHelper.autenticar(
                                activity = activity,
                                titulo = "Huella digital",
                                subtitulo = "Confirmá tu identidad",
                                onExito = {
                                    scope.launch {
                                        val rol = withContext(Dispatchers.IO) {
                                            AuthDataStore.rol(context)
                                        }
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
                    Text("Ingresar con huella digital")
                }
                TextButton(
                    onClick = {
                        scope.launch {
                            AuthDataStore.clear(context)
                            puedeHuella = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Usar otra cuenta (email o Google)")
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

@Preview(showBackground = true, name = "Auth - ingresar")
@Composable
fun PreviewAuthScreen() {
    PikuTheme {
        AuthScreen(onLoginCliente = {}, onLoginComercio = {})
    }
}
