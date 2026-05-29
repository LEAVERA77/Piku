package com.piku.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.piku.app.ui.components.MarkerInfoBottomSheet
import com.piku.app.ui.components.OsmdroidMapView
import com.piku.app.ui.viewmodel.MapaViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapaScreen(
    onVerDetalleComercio: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapaViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val permisoUbicacion = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    Box(modifier = modifier.fillMaxSize()) {
        if (!permisoUbicacion.status.isGranted) {
            Text(
                text = "Concedé permiso de ubicación para ver comercios cercanos",
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (uiState.cargando) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            OsmdroidMapView(
                comercios = uiState.comercios,
                centerLat = uiState.userLat,
                centerLon = uiState.userLon,
                onComercioClick = { viewModel.seleccionarComercio(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Mapa de ofertas", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(
                        value = uiState.busqueda,
                        onValueChange = { viewModel.buscarDireccion(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar con Nominatim…") },
                        singleLine = true
                    )
                }
            }
            if (uiState.resultadosBusqueda.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp)
                        .padding(top = 4.dp)
                ) {
                    LazyColumn {
                        items(uiState.resultadosBusqueda) { r ->
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

        uiState.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }

    uiState.comercioSeleccionado?.let { comercio ->
        MarkerInfoBottomSheet(
            comercio = comercio,
            onDismiss = { viewModel.seleccionarComercio(null) },
            onVerOfertas = {
                viewModel.seleccionarComercio(null)
                onVerDetalleComercio(comercio.id)
            }
        )
    }
}
