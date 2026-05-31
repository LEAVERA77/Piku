package com.piku.app.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.model.OfertaComercio
import com.piku.app.data.repository.OfertaRepository
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionOfertasScreen(
    onBack: () -> Unit,
    onNuevaOferta: () -> Unit,
    onEditarOferta: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { OfertaRepository(context) }

    var lista by remember { mutableStateOf<List<OfertaComercio>>(emptyList()) }
    var mensaje by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(true) }

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

    LaunchedEffect(Unit) { recargar() }

    val activas = lista.filter { it.activo }
    val inactivas = lista.filter { !it.activo }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis ofertas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNuevaOferta,
                containerColor = NaranjaPiku
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva oferta", tint = androidx.compose.ui.graphics.Color.White)
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
                Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            }
            if (cargando) {
                Text("Cargando ofertas…", modifier = Modifier.padding(16.dp))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Text("Activas (${activas.size})", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                    }
                    items(activas) { oferta ->
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
                                        recargar()
                                    } catch (e: Exception) {
                                        mensaje = e.message
                                    }
                                }
                            },
                            onStats = {
                                scope.launch {
                                    try {
                                        val s = repo.stats(oferta.id)
                                        mensaje = "${s.canjes} canjes · ${s.usuariosUnicos} clientes"
                                    } catch (e: Exception) {
                                        mensaje = e.message
                                    }
                                }
                            }
                        )
                    }
                    if (inactivas.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(12.dp))
                            Text("Archivadas (${inactivas.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        items(inactivas) { oferta ->
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
                                            recargar()
                                        } catch (e: Exception) {
                                            mensaje = e.message
                                        }
                                    }
                                },
                                onStats = {
                                    scope.launch {
                                        try {
                                            val s = repo.stats(oferta.id)
                                            mensaje = "${s.canjes} canjes históricos"
                                        } catch (e: Exception) {
                                            mensaje = e.message
                                        }
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

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

@Composable
private fun OfertaAdminCard(
    oferta: OfertaComercio,
    onEditar: () -> Unit,
    onDuplicar: () -> Unit,
    onToggleActiva: () -> Unit,
    onStats: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PikuPhotoImage(
                url = oferta.photoUrl(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp)),
                cornerRadius = 12.dp
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(oferta.nombre, style = MaterialTheme.typography.titleMedium)
                Text("${oferta.puntosRequeridos} pts · ${oferta.tipo ?: "oferta"}", style = MaterialTheme.typography.bodySmall)
                Text(
                    "${oferta.usosActuales ?: 0} canjes",
                    style = MaterialTheme.typography.labelMedium,
                    color = VerdePiku
                )
            }
            IconButton(onClick = onStats) {
                Text("Stats", style = MaterialTheme.typography.labelSmall, color = VerdePiku)
            }
            IconButton(onClick = onEditar) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = VerdePiku)
            }
            IconButton(onClick = onDuplicar) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar")
            }
            IconButton(onClick = onToggleActiva) {
                Icon(
                    if (oferta.activo) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (oferta.activo) "Pausar" else "Activar",
                    tint = NaranjaPiku
                )
            }
        }
    }
}
