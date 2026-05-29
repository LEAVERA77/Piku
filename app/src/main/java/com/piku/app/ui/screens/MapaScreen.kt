package com.piku.app.ui.screens

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
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
                comercios = uiState.comercios,
                centerLat = uiState.userLat,
                centerLon = uiState.userLon,
                onComercioClick = { viewModel.seleccionarComercio(it) },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (!tieneUbicacion) {
            Card(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Activá la ubicación para centrar el mapa en tu zona",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { permisosUbicacion.launchMultiplePermissionRequest() },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text("Permitir ubicación")
                    }
                    Text(
                        "El mapa se muestra igual; sin permiso usamos Buenos Aires como referencia.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
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
