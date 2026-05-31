package com.piku.app.ui.screens

import android.Manifest
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.theme.PikuTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.piku.app.ui.PikuChatSugerencias
import com.piku.app.ui.components.CharlaConPikuSheet
import com.piku.app.ui.components.MapaPanelCompacto
import com.piku.app.data.model.RecompensaPublica
import com.piku.app.data.repository.MapaRepository
import com.piku.app.ui.components.MarkerInfoBottomSheet
import com.piku.app.ui.components.OfertasBottomSheet
import com.piku.app.ui.components.OsmdroidMapView
import com.piku.app.ui.viewmodel.MapaViewModel

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapaScreen(
    onVerDetalleComercio: (String) -> Unit,
    onVerDetalleOferta: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapaViewModel = viewModel()
) {
    val context = LocalContext.current
    val mapaRepo = remember { MapaRepository(context) }
    var ofertasPin by remember { mutableStateOf<List<RecompensaPublica>>(emptyList()) }
    var cargandoOfertas by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val permisosUbicacion = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val tieneUbicacion = permisosUbicacion.allPermissionsGranted

    val direccionesMostrar = if (uiState.busquedaDireccion.length >= 2) {
        uiState.resultadosBusqueda
    } else {
        uiState.sugerenciasDireccion
    }

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
                modifier = Modifier.fillMaxSize()
            )
        }

        if (uiState.cargandoViewport) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 72.dp, end = 12.dp)
                    .size(24.dp),
                strokeWidth = 2.dp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            MapaPanelCompacto(
                busquedaNombre = uiState.busquedaNombre,
                busquedaDireccion = uiState.busquedaDireccion,
                contadorVisibles = uiState.contadorVisibles,
                buscandoOsm = uiState.buscandoComerciosOsm,
                rubros = uiState.rubros,
                rubrosSeleccionados = uiState.rubrosSeleccionados,
                expandido = uiState.panelExpandido,
                onToggleExpandido = viewModel::togglePanelExpandido,
                onBusquedaNombreChange = viewModel::setBusquedaNombre,
                onBusquedaDireccionChange = viewModel::buscarDireccion,
                onIrDireccion = viewModel::irADireccion,
                onToggleRubro = viewModel::toggleRubro
            )
            if (direccionesMostrar.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 120.dp)
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        direccionesMostrar.forEach { r ->
                            Text(
                                viewModel.etiquetaDireccion(r),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.centrarEnResultado(r) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
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
                    .padding(start = 12.dp, bottom = 88.dp)
            ) {
                Text("📍", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            SmallFloatingActionButton(
                onClick = { viewModel.centrarEnUsuario() },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 88.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Mi ubicación")
            }
        }

        FloatingActionButton(
            onClick = { viewModel.abrirChat() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 88.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("💬")
        }

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
                    .padding(bottom = 72.dp)
            )
        }
    }

    LaunchedEffect(uiState.comercioSeleccionado?.id) {
        val c = uiState.comercioSeleccionado
        if (c == null || c.esOpenStreetMap()) {
            ofertasPin = emptyList()
            return@LaunchedEffect
        }
        cargandoOfertas = true
        try {
            ofertasPin = mapaRepo.ofertasComercio(c.id).ofertas
        } catch (_: Exception) {
            ofertasPin = emptyList()
        } finally {
            cargandoOfertas = false
        }
    }

    uiState.comercioSeleccionado?.let { comercio ->
        if (comercio.esOpenStreetMap()) {
            MarkerInfoBottomSheet(
                comercio = comercio,
                rubros = uiState.rubros,
                onDismiss = { viewModel.seleccionarComercio(null) },
                onVerOfertas = {}
            )
        } else {
            OfertasBottomSheet(
                comercio = comercio,
                ofertas = ofertasPin,
                cargando = cargandoOfertas,
                onDismiss = { viewModel.seleccionarComercio(null) },
                onVerDetalle = { id ->
                    viewModel.seleccionarComercio(null)
                    onVerDetalleOferta(id)
                }
            )
        }
    }

    if (uiState.chatAbierto) {
        CharlaConPikuSheet(
            mensajes = uiState.mensajesChat,
            pregunta = uiState.preguntaChat,
            cargando = uiState.cargandoChat,
            comercioSugeridoId = uiState.comercioSugeridoId,
            preguntasSugeridas = PikuChatSugerencias.preguntas,
            onPreguntaChange = viewModel::setPreguntaChat,
            onEnviar = viewModel::enviarPreguntaChat,
            onPreguntaSugerida = viewModel::enviarPreguntaSugerida,
            onVerEnMapa = viewModel::seleccionarComercioSugerido,
            onDismiss = viewModel::cerrarChat
        )
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun PreviewMapaScreen() {
    PikuTheme {
        MapaScreen(onVerDetalleComercio = {}, onVerDetalleOferta = {})
    }
}
