package com.piku.app.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.data.network.ComercioRealtimeClient
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<Map<String, Any>?>(null) }
    var notificacionesNoLeidas by remember { mutableIntStateOf(0) }
    val repo = remember { ComercioRepository(context) }
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        try {
            stats = RetrofitInstance.api.estadisticasComercio()
        } catch (_: Exception) {
            stats = null
        }
        refrescarBadge()
    }

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
            PikuPhotoImage(
                url = PikuImages.comercioDefault,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                cornerRadius = 16.dp,
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(12.dp))
            Text("Tu negocio en Piku", style = MaterialTheme.typography.headlineSmall)
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
                    stats?.let { s ->
                        val data = s["estadisticas"] as? Map<*, *>
                        if (data.isNullOrEmpty()) {
                            Text("Sin datos aún", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            data.forEach { (k, v) ->
                                Text("$k: $v", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    } ?: Text("Sin estadísticas (verificá sesión)", style = MaterialTheme.typography.bodyMedium)
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
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDashboardComercioScreen() {
    PikuTheme {
        AdminDashboardScreen(onCerrarSesion = {})
    }
}
