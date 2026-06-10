package com.piku.app.ui.screens.comercio

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.model.PlanSuscripcion
import com.piku.app.data.model.SuscripcionEstadoResponse
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ComercioRepository(context) }

    var cargando by remember { mutableStateOf(true) }
    var cambiando by remember { mutableStateOf(false) }
    var estado by remember { mutableStateOf<SuscripcionEstadoResponse?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    fun recargar() {
        scope.launch {
            cargando = true
            error = null
            try {
                estado = withContext(Dispatchers.IO) { repo.obtenerEstadoSuscripcion() }
            } catch (e: Exception) {
                error = e.message
            } finally {
                cargando = false
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Suscripción") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            cargando -> CircularProgressIndicator(Modifier.padding(padding).padding(24.dp))
            error != null -> Column(
                Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
                BotonPiku(
                    texto = "Reintentar",
                    onClick = { recargar() },
                    estilo = EstiloBotonPiku.PRIMARIO
                )
            }
            else -> {
                val e = estado ?: return@Scaffold
                SuscripcionContent(
                    estado = e,
                    planes = e.planes.ifEmpty { planesDefault() },
                    cambiando = cambiando,
                    onSeleccionar = { plan ->
                        if (plan.id == e.plan || cambiando) return@SuscripcionContent
                        scope.launch {
                            cambiando = true
                            try {
                                val res = withContext(Dispatchers.IO) {
                                    repo.cambiarPlan(plan.id)
                                }
                                estado = res.estado ?: withContext(Dispatchers.IO) {
                                    repo.obtenerEstadoSuscripcion()
                                }
                                Toast.makeText(
                                    context,
                                    res.mensaje ?: "Plan actualizado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } catch (ex: Exception) {
                                Toast.makeText(
                                    context,
                                    ex.message ?: "No se pudo cambiar el plan",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                cambiando = false
                            }
                        }
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
internal fun SuscripcionContent(
    estado: SuscripcionEstadoResponse,
    planes: List<PlanSuscripcion>,
    cambiando: Boolean,
    onSeleccionar: (PlanSuscripcion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Elegí el plan que mejor se adapte a tu negocio.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
        }
        items(planes) { plan ->
            PlanCard(
                plan = plan,
                esActual = plan.id == estado.plan,
                cambiando = cambiando,
                onSeleccionar = { onSeleccionar(plan) }
            )
        }
    }
}

@Composable
private fun PlanCard(
    plan: PlanSuscripcion,
    esActual: Boolean,
    cambiando: Boolean,
    onSeleccionar: () -> Unit
) {
    val puntosTxt = plan.puntosMes?.let { "$it PP/mes" } ?: "Ilimitado"
    val ofertasTxt = plan.ofertasActivas?.let { "$it ofertas" } ?: "Ilimitado"
    val precioTxt = if (plan.precioUsd <= 0) "\$0/mes" else "\$${plan.precioUsd.toInt()} USD/mes"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esActual) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    plan.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (plan.destacado) Text("⭐", style = MaterialTheme.typography.titleMedium)
            }
            Text(precioTxt, style = MaterialTheme.typography.bodyLarge, color = NaranjaPiku)
            Spacer(Modifier.height(6.dp))
            Text("• $puntosTxt", style = MaterialTheme.typography.bodyMedium)
            Text("• $ofertasTxt activas", style = MaterialTheme.typography.bodyMedium)
            if (plan.destacado) {
                Text("• Destacado en el mapa", style = MaterialTheme.typography.bodySmall, color = VerdePiku)
            }
            Spacer(Modifier.height(12.dp))
            if (esActual) {
                Text("PLAN ACTUAL", fontWeight = FontWeight.Bold, color = VerdePiku)
            } else {
                BotonPiku(
                    texto = if (cambiando) "Actualizando…" else "ELEGIR ${plan.nombre.uppercase()}",
                    onClick = onSeleccionar,
                    modifier = Modifier.fillMaxWidth(),
                    habilitado = !cambiando,
                    estilo = if (plan.id == "pro") EstiloBotonPiku.PRIMARIO else EstiloBotonPiku.CONTORNO
                )
            }
        }
    }
}

private fun planesDefault(): List<PlanSuscripcion> = listOf(
    PlanSuscripcion("gratuito", "Gratuito", 0.0, 500, 2, false),
    PlanSuscripcion("basico", "Básico", 5.0, 5000, 10, false),
    PlanSuscripcion("pro", "Pro", 15.0, null, null, true)
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewSuscripcionScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Suscripción") })
            }
        ) { padding ->
            SuscripcionContent(
                estado = PreviewMocks.suscripcionEstado,
                planes = PreviewMocks.suscripcionEstado.planes,
                cambiando = false,
                onSeleccionar = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
