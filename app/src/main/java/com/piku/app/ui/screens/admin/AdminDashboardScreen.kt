package com.piku.app.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.piku.app.data.model.EstadisticasComercioResponse
import com.piku.app.data.network.ComercioRealtimeClient
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.data.model.SuscripcionEstadoResponse
import androidx.compose.material.icons.Icons
import com.piku.app.ui.components.TarjetaSuscripcionProgreso
import com.piku.app.utils.rememberImagePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onCerrarSesion: () -> Unit,
    onSuscripcion: () -> Unit = {},
    onInsights: () -> Unit = {}
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<EstadisticasComercioResponse?>(null) }
    var errorPanel by remember { mutableStateOf<String?>(null) }
    var notificacionesNoLeidas by remember { mutableIntStateOf(0) }
    var logoUrl by remember { mutableStateOf<String?>(null) }
    var nombreComercio by remember { mutableStateOf<String?>(null) }
    var subiendoLogo by remember { mutableStateOf(false) }
    var suscripcion by remember { mutableStateOf<SuscripcionEstadoResponse?>(null) }
    val repo = remember { ComercioRepository(context) }
    val scope = rememberCoroutineScope()

    val elegirLogo = rememberImagePicker { uri ->
        scope.launch {
            subiendoLogo = true
            try {
                val url = withContext(Dispatchers.IO) { repo.subirLogo(uri) }
                logoUrl = url
                Toast.makeText(context, "Logo actualizado", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Error al subir logo", Toast.LENGTH_LONG).show()
            } finally {
                subiendoLogo = false
            }
        }
    }

    fun refrescarBadge() {
        scope.launch {
            try {
                notificacionesNoLeidas = withContext(Dispatchers.IO) {
                    repo.contarNotificacionesNoLeidas()
                }
            } catch (_: Exception) {
                notificacionesNoLeidas = 0
            }
        }
    }

    fun cargarPanel() {
        scope.launch {
            errorPanel = null
            try {
                stats = withContext(Dispatchers.IO) { repo.obtenerEstadisticas() }
            } catch (e: Exception) {
                stats = null
                errorPanel = e.message ?: "No pudimos cargar las estadísticas"
            }
            try {
                val perfil = withContext(Dispatchers.IO) { repo.obtenerPerfil() }
                logoUrl = perfil.comercio?.logoUrl
                nombreComercio = perfil.comercio?.nombre
            } catch (e: Exception) {
                if (errorPanel == null) errorPanel = e.message
            }
            try {
                suscripcion = withContext(Dispatchers.IO) { repo.obtenerEstadoSuscripcion() }
            } catch (_: Exception) {
                suscripcion = null
            }
            refrescarBadge()
        }
    }

    LaunchedEffect(Unit) { cargarPanel() }

    DisposableEffect(Unit) {
        val realtime = ComercioRealtimeClient(context) { refrescarBadge() }
        realtime.conectar()
        onDispose { realtime.desconectar() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            suscripcion?.let { sub ->
                TarjetaSuscripcionProgreso(
                    estado = sub,
                    onActualizarPlan = onSuscripcion
                )
                Spacer(Modifier.height(12.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(enabled = !subiendoLogo) { elegirLogo() },
                contentAlignment = Alignment.Center
            ) {
                PikuPhotoImage(
                    url = logoUrl?.takeIf { it.isNotBlank() } ?: PikuImages.comercioDefault,
                    contentDescription = "Logo del comercio",
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    contentScale = ContentScale.Crop
                )
                if (subiendoLogo) {
                    CircularProgressIndicator(color = NaranjaPiku)
                }
            }
            OutlinedButton(
                onClick = { elegirLogo() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !subiendoLogo
            ) {
                Text("Cambiar logo del comercio")
            }
            Spacer(Modifier.height(12.dp))
            Text(
                nombreComercio ?: "Tu negocio en Piku",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                "Usá la pestaña Catálogo para publicar artículos con varias fotos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Resumen", style = MaterialTheme.typography.titleMedium, color = VerdePiku)
                    Spacer(Modifier.height(8.dp))
                    val data = stats?.estadisticas
                    when {
                        data != null -> {
                            FilaEstadistica("📲 QR escaneados", data.qrUsados)
                            FilaEstadistica("⭐ Puntos otorgados", data.puntosOtorgados)
                            FilaEstadistica("🎁 Canjes realizados", data.canjesRealizados)
                            FilaEstadistica("👥 Clientes únicos", data.clientesUnicos)
                        }
                        errorPanel != null -> {
                            Text(
                                errorPanel ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { cargarPanel() }) {
                                Text("Reintentar")
                            }
                        }
                        else -> Text("Sin datos aún", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (notificacionesNoLeidas > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🔔 $notificacionesNoLeidas notificación(es) sin leer — pestaña Más",
                            style = MaterialTheme.typography.labelLarge,
                            color = NaranjaPiku
                        )
                    }
                }
            }
            val ultimas = stats?.ultimasTransacciones.orEmpty()
            if (ultimas.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Última actividad",
                            style = MaterialTheme.typography.titleMedium,
                            color = VerdePiku
                        )
                        Spacer(Modifier.height(8.dp))
                        ultimas.take(5).forEach { t ->
                            val signo = if (t.puntos >= 0) "+" else ""
                            Text(
                                "${t.cliente ?: "Cliente"} · $signo${t.puntos} PP — ${t.descripcion ?: t.tipo.orEmpty()}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onInsights, modifier = Modifier.fillMaxWidth()) {
                Text("Ver insights del mes")
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
private fun FilaEstadistica(etiqueta: String, valor: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge)
        Text(
            valor.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = NaranjaPiku
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDashboardComercioScreen() {
    PikuTheme {
        AdminDashboardScreen(onCerrarSesion = {})
    }
}
