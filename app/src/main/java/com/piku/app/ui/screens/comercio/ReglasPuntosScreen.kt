package com.piku.app.ui.screens.comercio

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.theme.NaranjaPiku
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReglasPuntosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ComercioRepository(context) }

    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var infoSistema by remember { mutableStateOf<String?>(null) }

    var montoMinimo by remember { mutableStateOf("0") }
    var puntosFijos by remember { mutableStateOf("10") }
    var maxPorDia by remember { mutableStateOf("500") }
    var activo by remember { mutableStateOf(true) }

    fun recargar() {
        scope.launch {
            cargando = true
            error = null
            try {
                val res = withContext(Dispatchers.IO) { repo.obtenerReglasPuntos() }
                val r = res.reglas
                montoMinimo = (r.montoMinimo ?: 0.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                puntosFijos = (r.puntosFijos ?: 0).toString()
                maxPorDia = (r.maxPuntosPorDia ?: 500).toString()
                activo = r.activo != false
                infoSistema = res.sistemaPikuPoints?.get("regla")
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
                title = { Text("Política de puntos") },
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
            else -> ReglasPuntosFormContent(
                montoMinimo = montoMinimo,
                onMontoMinimoChange = { montoMinimo = it.filter { c -> c.isDigit() || c == '.' } },
                puntosFijos = puntosFijos,
                onPuntosFijosChange = { puntosFijos = it.filter { c -> c.isDigit() } },
                maxPorDia = maxPorDia,
                onMaxPorDiaChange = { maxPorDia = it.filter { c -> c.isDigit() } },
                activo = activo,
                onActivoChange = { activo = it },
                infoSistema = infoSistema,
                error = error,
                guardando = guardando,
                onGuardar = {
                    scope.launch {
                        guardando = true
                        error = null
                        try {
                            withContext(Dispatchers.IO) {
                                repo.guardarReglasPuntos(
                                    montoMinimo = montoMinimo.toDoubleOrNull() ?: 0.0,
                                    puntosFijos = puntosFijos.toIntOrNull() ?: 0,
                                    maxPuntosPorDia = maxPorDia.toIntOrNull() ?: 500,
                                    activo = activo
                                )
                            }
                            Toast.makeText(context, "Política actualizada", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            guardando = false
                        }
                    }
                },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
internal fun ReglasPuntosFormContent(
    montoMinimo: String,
    onMontoMinimoChange: (String) -> Unit,
    puntosFijos: String,
    onPuntosFijosChange: (String) -> Unit,
    maxPorDia: String,
    onMaxPorDiaChange: (String) -> Unit,
    activo: Boolean,
    onActivoChange: (Boolean) -> Unit,
    infoSistema: String?,
    error: String?,
    guardando: Boolean,
    onGuardar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        Modifier
            .fillMaxSize()
            .then(modifier)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Configurá cómo tus clientes acumulan y canjean Piku Points en tu local.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        infoSistema?.let { regla ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Sistema Piku Points", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(regla, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        OutlinedTextField(
            value = montoMinimo,
            onValueChange = onMontoMinimoChange,
            label = { Text("Monto mínimo de compra (ARS)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = puntosFijos,
            onValueChange = onPuntosFijosChange,
            label = { Text("Puntos fijos por visita (opcional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = maxPorDia,
            onValueChange = onMaxPorDiaChange,
            label = { Text("Máximo de puntos por cliente por día") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Programa activo", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Si está desactivado, no se otorgan puntos en caja.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(checked = activo, onCheckedChange = onActivoChange)
            }
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        BotonPiku(
            texto = if (guardando) "Guardando…" else "Guardar política",
            onClick = onGuardar,
            modifier = Modifier.fillMaxWidth(),
            habilitado = !guardando,
            estilo = EstiloBotonPiku.PRIMARIO
        )

        Text(
            "Los puntos requeridos para cada publicación se configuran en el catálogo.",
            style = MaterialTheme.typography.bodySmall,
            color = NaranjaPiku
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewReglasPuntosScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Política de puntos") })
            }
        ) { padding ->
            ReglasPuntosFormContent(
                montoMinimo = "500",
                onMontoMinimoChange = {},
                puntosFijos = "10",
                onPuntosFijosChange = {},
                maxPorDia = "500",
                onMaxPorDiaChange = {},
                activo = true,
                onActivoChange = {},
                infoSistema = "1 punto por cada $100 de compra (mínimo $500).",
                error = null,
                guardando = false,
                onGuardar = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
