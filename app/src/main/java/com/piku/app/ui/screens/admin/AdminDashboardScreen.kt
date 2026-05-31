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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.media.PikuImages

@Composable
fun AdminDashboardScreen(
    onOfertas: () -> Unit,
    onConfigEnvios: () -> Unit = {},
    onGenerarQr: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var stats by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        try {
            stats = RetrofitInstance.api.estadisticasComercio()
        } catch (_: Exception) {
            stats = null
        }
    }

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        PikuPhotoImage(
            url = PikuImages.comercioDefault,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
            cornerRadius = 16.dp,
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))
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
        Button(onClick = onConfigEnvios, modifier = Modifier.fillMaxWidth()) { Text("Configurar envíos") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onGenerarQr, modifier = Modifier.fillMaxWidth()) { Text("Generar QR") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onCerrarSesion, modifier = Modifier.fillMaxWidth()) { Text("Cerrar sesión") }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDashboardComercioScreen() {
    PikuTheme {
        AdminDashboardScreen(
            onOfertas = {},
            onConfigEnvios = {},
            onGenerarQr = {},
            onCerrarSesion = {}
        )
    }
}
