package com.piku.app.ui.screens

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.piku.app.ui.components.CharlaConPikuSheet
import com.piku.app.ui.components.MarkerInfoBottomSheet
import com.piku.app.ui.components.OsmdroidMapView
import com.piku.app.ui.viewmodel.MapaViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    onVerDetalleComercio: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val permisosUbicacion = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val tieneUbicacion = permisosUbicacion.allPermissionsGranted

    LaunchedEffect(tieneUbicacion) {
        viewModel.cargarUbicacionYComercios(conUbicacion = tieneUbicacion)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.cargando) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            OsmdroidMapView(
                comercios = uiState.comerciosVisibles,
                centerLat = uiState.userLat,
                centerLon = uiState.userLon,
                userLat = if (uiState.tieneUbicacionReal) uiState.userLat else null,
                userLon = if (uiState.tieneUbicacionReal) uiState.userLon else null,
                onComercioClick = { viewModel.seleccionarComercio(it) },
                onViewportChanged = viewModel::onViewportChanged,
                zoomLevel = uiState.zoomMapa,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(0.dp))
            )
        }

        if (uiState.cargandoViewport) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 140.dp, end = 16.dp)
                    .size(28.dp),
                strokeWidth = 2.dp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Mapa de ofertas", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = uiState.busquedaNombre,
                        onValueChange = { viewModel.setBusquedaNombre(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar comercio por nombre") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.busquedaDireccion,
                        onValueChange = { viewModel.buscarDireccion(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        placeholder = { Text("Ir a una dirección…") },
                        singleLine = true
                    )
                    Text(
                        "${uiState.contadorVisibles} comercios visibles",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    if (uiState.rubros.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.rubros.forEach { rubro ->
                                val selected = uiState.rubrosSeleccionados.contains(rubro.id)
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.toggleRubro(rubro.id) },
                                    label = { Text(rubro.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }
            if (uiState.resultadosBusqueda.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        uiState.resultadosBusqueda.forEach { r ->
                            Text(
                                r.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.centrarEnResultado(r) }
                                    .padding(10.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }

        if (!tieneUbicacion) {
            SmallFloatingActionButton(
                onClick = { permisosUbicacion.launchMultiplePermissionRequest() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 88.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text("📍", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            SmallFloatingActionButton(
                onClick = { viewModel.centrarEnUsuario() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 88.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
            }
        }

        FloatingActionButton(
            onClick = { viewModel.abrirChat() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 88.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Text("💬", style = MaterialTheme.typography.titleMedium)
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .padding(bottom = 72.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    uiState.comercioSeleccionado?.let { comercio ->
        MarkerInfoBottomSheet(
            comercio = comercio,
            rubros = uiState.rubros,
            onDismiss = { viewModel.seleccionarComercio(null) },
            onVerOfertas = {
                viewModel.seleccionarComercio(null)
                onVerDetalleComercio(comercio.id)
            }
        )
    }

    if (uiState.chatAbierto) {
        CharlaConPikuSheet(
            mensajes = uiState.mensajesChat,
            pregunta = uiState.preguntaChat,
            cargando = uiState.cargandoChat,
            comercioSugeridoId = uiState.comercioSugeridoId,
            onPreguntaChange = viewModel::setPreguntaChat,
            onEnviar = viewModel::enviarPreguntaChat,
            onVerEnMapa = viewModel::seleccionarComercioSugerido,
            onDismiss = viewModel::cerrarChat
        )
    }
}
