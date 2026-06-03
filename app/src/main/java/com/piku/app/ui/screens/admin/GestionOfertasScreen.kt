package com.piku.app.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.data.model.OfertaComercio
import com.piku.app.data.model.OfertaStatsResponse
import com.piku.app.data.repository.OfertaRepository
import com.piku.app.ui.components.IndicadorFotosBadge
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionOfertasScreen(
    onBack: () -> Unit,
    onNuevaOferta: () -> Unit,
    onEditarOferta: (String) -> Unit,
    modoTab: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { OfertaRepository(context) }

    var lista by remember { mutableStateOf<List<OfertaComercio>>(emptyList()) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var statsDialog by remember { mutableStateOf<StatsDialogState?>(null) }

    fun recargar() {
        scope.launch {
            cargando = true
            try {
                lista = repo.listar()
            } catch (e: Exception) {
                mensaje = e.message
            } finally {
                cargando = false
            }
        }
    }

    fun mostrarEstadisticas(oferta: OfertaComercio) {
        statsDialog = StatsDialogState(oferta.nombre, cargando = true)
        scope.launch {
            try {
                val s = repo.stats(oferta.id)
                statsDialog = StatsDialogState(oferta.nombre, cargando = false, stats = s)
            } catch (e: Exception) {
                statsDialog = null
                mensaje = e.message
            }
        }
    }

    LaunchedEffect(Unit) { recargar() }

    val activas = lista.filter { it.activo }
    val inactivas = lista.filter { !it.activo }

    statsDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { statsDialog = null },
            title = { Text("Estadísticas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        dialog.nombreOferta,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (dialog.cargando) {
                        Text("Cargando…", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        dialog.stats?.let { s ->
                            Text("Canjes totales: ${s.canjes}", style = MaterialTheme.typography.bodyLarge)
                            Text("Clientes únicos: ${s.usuariosUnicos}", style = MaterialTheme.typography.bodyLarge)
                            Text("Usos registrados: ${s.usosActuales}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { statsDialog = null }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catálogo") },
                navigationIcon = {
                    if (!modoTab) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaOferta,
                containerColor = NaranjaPiku
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva oferta", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            mensaje?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
            }
            Text(
                "Publicá artículos con foto, descuentos y puntos para canje.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (cargando) {
                Text("Cargando catálogo…", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Text("Activas (${activas.size})", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(activas, key = { it.id }) { oferta ->
                        OfertaAdminCard(
                            oferta = oferta,
                            onEditar = { onEditarOferta(oferta.id) },
                            onDuplicar = {
                                scope.launch {
                                    try {
                                        repo.duplicar(oferta.id)
                                        mensaje = "Oferta duplicada"
                                        recargar()
                                    } catch (e: Exception) {
                                        mensaje = e.message
                                    }
                                }
                            },
                            onToggleActiva = {
                                scope.launch {
                                    try {
                                        repo.actualizar(oferta.id, mapOf("activo" to false))
                                        Toast.makeText(context, "Publicación pausada", Toast.LENGTH_SHORT).show()
                                        recargar()
                                    } catch (e: Exception) {
                                        mensaje = e.message
                                    }
                                }
                            },
                            onEstadisticas = { mostrarEstadisticas(oferta) }
                        )
                    }
                    if (inactivas.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("Archivadas (${inactivas.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(inactivas, key = { it.id }) { oferta ->
                            OfertaAdminCard(
                                oferta = oferta,
                                onEditar = { onEditarOferta(oferta.id) },
                                onDuplicar = {
                                    scope.launch {
                                        try {
                                            repo.duplicar(oferta.id)
                                            mensaje = "Copia creada"
                                            recargar()
                                        } catch (e: Exception) {
                                            mensaje = e.message
                                        }
                                    }
                                },
                                onToggleActiva = {
                                    scope.launch {
                                        try {
                                            repo.actualizar(oferta.id, mapOf("activo" to true))
                                            Toast.makeText(context, "Publicación activada", Toast.LENGTH_SHORT).show()
                                            recargar()
                                        } catch (e: Exception) {
                                            mensaje = e.message
                                        }
                                    }
                                },
                                onEstadisticas = { mostrarEstadisticas(oferta) }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

private data class StatsDialogState(
    val nombreOferta: String,
    val cargando: Boolean = false,
    val stats: OfertaStatsResponse? = null
)

@Preview(showBackground = true)
@Composable
fun PreviewGestionOfertasScreen() {
    PikuTheme {
        GestionOfertasScreen(
            onBack = {},
            onNuevaOferta = {},
            onEditarOferta = {}
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfertaAdminCard(
    oferta: OfertaComercio,
    onEditar: () -> Unit,
    onDuplicar: () -> Unit,
    onToggleActiva: () -> Unit,
    onEstadisticas: () -> Unit
) {
    val tipoLabel = when (oferta.tipo) {
        "descuento_porcentaje" -> "Descuento ${oferta.porcentajeDescuento ?: 0}%"
        "producto_gratis" -> "Producto gratis"
        "2x1" -> "2×1"
        else -> oferta.tipo?.replace('_', ' ')?.replaceFirstChar { it.uppercase() } ?: "Oferta"
    }
    val estadoColor = if (oferta.activo) VerdePiku else MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box {
                    PikuPhotoImage(
                        url = oferta.photoUrl(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        cornerRadius = 12.dp
                    )
                    IndicadorFotosBadge(
                        cantidad = oferta.cantidadFotos,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        oferta.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${oferta.puntosRequeridos} puntos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NaranjaPiku,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        tipoLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    oferta.descripcion?.takeIf { it.isNotBlank() }?.let { desc ->
                        Spacer(Modifier.height(2.dp))
                        Text(
                            desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (oferta.activo) "Activa" else "Pausada",
                            style = MaterialTheme.typography.labelMedium,
                            color = estadoColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            " · ${oferta.usosActuales ?: 0} canjes",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Spacer(Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onEstadisticas) {
                    Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Estadísticas")
                }
                TextButton(onClick = onEditar) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar")
                }
                TextButton(onClick = onDuplicar) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Duplicar")
                }
                TextButton(onClick = onToggleActiva) {
                    Icon(
                        if (oferta.activo) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = NaranjaPiku
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (oferta.activo) "Pausar" else "Activar",
                        color = NaranjaPiku
                    )
                }
            }
        }
    }
}
