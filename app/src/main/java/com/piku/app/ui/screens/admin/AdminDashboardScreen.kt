package com.piku.app.ui.screens.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piku.app.data.network.RetrofitInstance

@Composable
fun AdminDashboardScreen(
    onOfertas: () -> Unit,
    onGenerarQr: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val context = LocalContext.current
    var stats by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        try {
            stats = RetrofitInstance.api.estadisticasComercio()
        } catch (_: Exception) {
            stats = null
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Panel comercio", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        stats?.let { s ->
            val data = s["estadisticas"] as? Map<*, *>
            data?.forEach { (k, v) ->
                Text("$k: $v", style = MaterialTheme.typography.bodyLarge)
            }
        } ?: Text("Sin estadísticas (verificá sesión de comercio)")
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOfertas, modifier = Modifier.fillMaxWidth()) { Text("Gestionar ofertas") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onGenerarQr, modifier = Modifier.fillMaxWidth()) { Text("Generar QR") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}
