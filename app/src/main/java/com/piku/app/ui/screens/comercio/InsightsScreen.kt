package com.piku.app.ui.screens.comercio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.model.ComercioInsightsResponse
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.InsightsViewModel
import kotlin.math.max

private val etiquetasTipo = mapOf(
    "descuento" to "Descuento",
    "producto_gratis" to "Producto gratis",
    "2x1" to "2x1",
    "envio_gratis" to "Envío gratis"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refrescar() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        when {
            uiState.cargando && uiState.insights == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VerdePiku)
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
                }
            }
            uiState.insights != null -> {
                InsightsContent(
                    insights = uiState.insights!!,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun InsightsContent(insights: ComercioInsightsResponse, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiCard(
                    titulo = "Puntos este mes",
                    valor = "${insights.puntosEntregadosMes}",
                    subtitulo = variacionTexto(insights.variacionPuntos),
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    titulo = "Canjes",
                    valor = "${insights.ofertasCanjeadas.total}",
                    subtitulo = "mes actual",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            KpiCard(
                titulo = "Clientes recurrentes",
                valor = "${insights.clientesRecurrentes}",
                subtitulo = "${insights.porcentajeRecurrentes}% del total",
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NaranjaPiku.copy(alpha = 0.1f))
            ) {
                Text(
                    insights.recomendacion,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (insights.ofertasCanjeadas.porTipo.isNotEmpty()) {
            item {
                Text("Canjes por tipo de oferta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                val maxVal = max(1, insights.ofertasCanjeadas.porTipo.values.maxOrNull() ?: 1)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    insights.ofertasCanjeadas.porTipo.entries
                        .sortedByDescending { it.value }
                        .forEach { (tipo, count) ->
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(etiquetasTipo[tipo] ?: tipo)
                                    Text("$count", fontWeight = FontWeight.Bold)
                                }
                                LinearProgressIndicator(
                                    progress = { count.toFloat() / maxVal },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = VerdePiku
                                )
                            }
                        }
                }
            }
        }

        if (insights.topClientes.isNotEmpty()) {
            item {
                Text("Top clientes del mes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            insights.topClientes.forEachIndexed { index, cliente ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("#${index + 1} ${cliente.nombre ?: "Cliente"}", fontWeight = FontWeight.SemiBold)
                                cliente.email?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("${cliente.puntosGanados} PP", color = VerdePiku, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun KpiCard(titulo: String, valor: String, subtitulo: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = VerdePiku)
            Text(subtitulo, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun variacionTexto(variacion: Double): String = when {
    variacion > 0 -> "+${variacion}% vs mes anterior"
    variacion < 0 -> "${variacion}% vs mes anterior"
    else -> "igual que mes anterior"
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewInsightsScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Insights") })
            }
        ) { padding ->
            InsightsContent(
                insights = PreviewMocks.insights,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
