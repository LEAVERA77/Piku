package com.piku.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.data.model.RecompensaDetalleResponse
import com.piku.app.data.repository.MapaRepository
import com.piku.app.data.repository.UsuarioRepository
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.theme.VerdePiku
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleOfertaScreen(
    recompensaId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val usuarioRepo = remember { UsuarioRepository(context) }

    var detalle by remember { mutableStateOf<RecompensaDetalleResponse?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var puntosSaldo by remember { mutableIntStateOf(0) }
    var esCliente by remember { mutableStateOf(false) }
    var mostrarConfirmacion by remember { mutableStateOf(false) }
    var canjeando by remember { mutableStateOf(false) }
    var codigoCanje by remember { mutableStateOf<String?>(null) }

    val cloud = ConfigLoader.cloudinaryCloudName(context)

    LaunchedEffect(recompensaId) {
        cargando = true
        esCliente = AuthDataStore.rol(context) == "cliente"
        try {
            detalle = MapaRepository(context).detalleRecompensa(recompensaId)
            if (esCliente) {
                puntosSaldo = usuarioRepo.obtenerSaldo().puntos
            }
        } catch (e: Exception) {
            error = e.message
        } finally {
            cargando = false
        }
    }

    fun ejecutarCanje() {
        scope.launch {
            canjeando = true
            try {
                val res = usuarioRepo.canjearRecompensa(recompensaId)
                puntosSaldo = res.puntosRestantes ?: puntosSaldo
                codigoCanje = res.codigoCanje
                mostrarConfirmacion = false
                snackbar.showSnackbar(res.mensaje)
            } catch (e: Exception) {
                snackbar.showSnackbar(e.message ?: "No se pudo canjear")
            } finally {
                canjeando = false
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(detalle?.recompensa?.nombre ?: "Oferta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            cargando -> CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            error != null -> Text(error!!, Modifier.padding(padding).padding(16.dp))
            detalle != null -> {
                val oferta = detalle!!.recompensa
                val puedeCanjear = esCliente && puntosSaldo >= oferta.puntosRequeridos && codigoCanje == null

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    PikuPhotoImage(
                        url = oferta.photoUrl(cloud),
                        contentDescription = oferta.nombre,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        cornerRadius = 16.dp,
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(oferta.nombre, style = MaterialTheme.typography.headlineMedium)
                    detalle!!.comercio?.nombre?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (esCliente) {
                        Text(
                            "Tu saldo: $puntosSaldo pts",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    Text(
                        "${oferta.puntosRequeridos} puntos necesarios",
                        style = MaterialTheme.typography.titleMedium,
                        color = VerdePiku,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    oferta.descripcion?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }
                    oferta.condiciones?.let {
                        Spacer(Modifier.height(12.dp))
                        Text("Condiciones", style = MaterialTheme.typography.titleSmall)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (!oferta.vigenciaDesde.isNullOrBlank() || !oferta.vigenciaHasta.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Vigencia: ${oferta.vigenciaDesde ?: "—"} → ${oferta.vigenciaHasta ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    codigoCanje?.let { codigo ->
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Código de canje",
                            style = MaterialTheme.typography.titleSmall,
                            color = VerdePiku
                        )
                        Text(
                            codigo,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Mostrá este código en el comercio para usar tu beneficio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (esCliente && codigoCanje == null) {
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { mostrarConfirmacion = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = puedeCanjear && !canjeando,
                            colors = ButtonDefaults.buttonColors(containerColor = VerdePiku)
                        ) {
                            if (canjeando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Canjear")
                            }
                        }
                        if (!puedeCanjear && puntosSaldo < oferta.puntosRequeridos) {
                            Text(
                                "Te faltan ${oferta.puntosRequeridos - puntosSaldo} puntos para canjear.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarConfirmacion && detalle != null) {
        val oferta = detalle!!.recompensa
        AlertDialog(
            onDismissRequest = { if (!canjeando) mostrarConfirmacion = false },
            title = { Text("Confirmar canje") },
            text = {
                Text(
                    "¿Canjear \"${oferta.nombre}\" por ${oferta.puntosRequeridos} puntos? " +
                        "Te quedarán ${puntosSaldo - oferta.puntosRequeridos} pts."
                )
            },
            confirmButton = {
                Button(
                    onClick = { ejecutarCanje() },
                    enabled = !canjeando,
                    colors = ButtonDefaults.buttonColors(containerColor = VerdePiku)
                ) {
                    Text(if (canjeando) "Canjeando…" else "Canjear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarConfirmacion = false },
                    enabled = !canjeando
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}
