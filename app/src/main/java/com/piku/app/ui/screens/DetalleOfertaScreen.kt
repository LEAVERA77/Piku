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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleOfertaScreen(
    recompensaId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var detalle by remember { mutableStateOf<RecompensaDetalleResponse?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val cloud = ConfigLoader.cloudinaryCloudName(context)
    val esCliente = remember { mutableStateOf(false) }

    LaunchedEffect(recompensaId) {
        cargando = true
        esCliente.value = AuthDataStore.rol(context) == "cliente"
        try {
            detalle = MapaRepository(context).detalleRecompensa(recompensaId)
        } catch (e: Exception) {
            error = e.message
        } finally {
            cargando = false
        }
    }

    Scaffold(
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
                    if (esCliente.value) {
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { /* canje vía flujo existente en Canjes */ },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false
                        ) {
                            Text("Canjear (próximamente desde aquí)", color = NaranjaPiku)
                        }
                        Text(
                            "Usá la pestaña Canjes para canjear con tus puntos.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
