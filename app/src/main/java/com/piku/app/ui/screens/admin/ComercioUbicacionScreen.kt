package com.piku.app.ui.screens.admin

import android.Manifest
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.piku.app.data.CerritoGeo
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.SelectorUbicacionMapa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ComercioUbicacionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { ComercioRepository(context) }

    var lat by remember { mutableDoubleStateOf(CerritoGeo.CENTRO_LAT) }
    var lon by remember { mutableDoubleStateOf(CerritoGeo.CENTRO_LON) }
    var direccion by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(true) }
    var guardando by remember { mutableStateOf(false) }

    val permisosUbicacion = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        cargando = true
        try {
            val perfil = withContext(Dispatchers.IO) { repo.obtenerPerfil() }
            perfil.comercio?.let { c ->
                if (c.lat != null && c.lon != null) {
                    lat = c.lat
                    lon = c.lon
                }
                direccion = c.direccion.orEmpty()
            }
        } catch (_: Exception) {
            // valores por defecto
        } finally {
            cargando = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubicación del negocio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (cargando) {
            CircularProgressIndicator(Modifier.padding(padding).padding(24.dp))
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Marcá dónde está tu local. Los clientes verán el pin en el mapa.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            SelectorUbicacionMapa(
                lat = lat,
                lon = lon,
                onCentroCambiado = { nuevaLat, nuevaLon ->
                    lat = nuevaLat
                    lon = nuevaLon
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Coordenadas: ${"%.5f".format(lat)}, ${"%.5f".format(lon)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            BotonPiku(
                texto = "Usar mi ubicación actual",
                onClick = {
                    if (!permisosUbicacion.allPermissionsGranted) {
                        permisosUbicacion.launchMultiplePermissionRequest()
                        return@BotonPiku
                    }
                    scope.launch {
                        try {
                            val coords = withContext(Dispatchers.IO) { repo.obtenerUbicacionGps() }
                            if (coords != null) {
                                lat = coords.first
                                lon = coords.second
                            } else {
                                Toast.makeText(context, "No pudimos obtener tu ubicación", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Error de GPS", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                icono = Icons.Default.MyLocation
            )
            Spacer(Modifier.height(12.dp))
            BotonPiku(
                texto = if (guardando) "Guardando…" else "Guardar ubicación",
                onClick = {
                    if (guardando) return@BotonPiku
                    scope.launch {
                        guardando = true
                        try {
                            withContext(Dispatchers.IO) {
                                repo.actualizarUbicacion(lat, lon, direccion.trim().ifBlank { null })
                            }
                            Toast.makeText(context, "Ubicación guardada", Toast.LENGTH_SHORT).show()
                            onBack()
                        } catch (e: Exception) {
                            Toast.makeText(
                                context,
                                e.message ?: "No se pudo guardar",
                                Toast.LENGTH_LONG
                            ).show()
                        } finally {
                            guardando = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
