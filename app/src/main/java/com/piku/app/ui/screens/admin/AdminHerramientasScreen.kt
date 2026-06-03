package com.piku.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Loyalty
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku

private data class HerramientaItem(
    val titulo: String,
    val subtitulo: String,
    val icono: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHerramientasScreen(
    onGenerarQr: () -> Unit,
    onConfigEnvios: () -> Unit,
    onNotificaciones: () -> Unit,
    onHistorialCanjes: () -> Unit,
    onUbicacion: () -> Unit,
    onSuscripcion: () -> Unit,
    onReglasPuntos: () -> Unit
) {
    val items = listOf(
        HerramientaItem("Generar QR", "Sumar puntos en caja", Icons.Default.QrCode, onGenerarQr),
        HerramientaItem("Suscripción", "Plan y límites mensuales", Icons.Default.Star, onSuscripcion),
        HerramientaItem("Política de puntos", "Canjes y límites diarios", Icons.Default.Loyalty, onReglasPuntos),
        HerramientaItem("Ubicación", "Marcar local en el mapa", Icons.Default.Place, onUbicacion),
        HerramientaItem("Envíos", "Domicilio y teléfono", Icons.Default.LocalShipping, onConfigEnvios),
        HerramientaItem("Notificaciones", "Canjes en vivo", Icons.Default.Notifications, onNotificaciones),
        HerramientaItem("Historial", "Canjes realizados", Icons.Default.History, onHistorialCanjes)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Herramientas") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Card(
                    onClick = item.onClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            item.icono,
                            contentDescription = null,
                            tint = VerdePiku,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(item.titulo, style = MaterialTheme.typography.titleMedium)
                        Text(
                            item.subtitulo,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
