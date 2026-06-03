package com.piku.app.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.piku.app.data.datastore.AppPreferences
import com.piku.app.data.security.InstallSessionGuard
import androidx.compose.runtime.rememberCoroutineScope
import com.piku.app.ui.theme.PikuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PERMISOS_UBICACION = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * Un solo aviso de ubicación: sin diálogo automático ni superposición que bloquee la app.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UbicacionPermisoGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permisos = rememberMultiplePermissionsState(PERMISOS_UBICACION)
    val concedido = permisos.allPermissionsGranted
    var mostrarAviso by remember { mutableStateOf(false) }

    LaunchedEffect(concedido) {
        if (concedido) {
            mostrarAviso = false
            return@LaunchedEffect
        }
        delay(2_500)
        if (!permisos.allPermissionsGranted) {
            mostrarAviso = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (!concedido && mostrarAviso) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 80.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ubicación para Piku",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Permití el acceso para ver comercios cerca tuyo en el mapa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start
                    )
                    BotonPiku(
                        texto = "Permitir ubicación",
                        onClick = {
                            permisos.launchMultiplePermissionRequest()
                            mostrarAviso = false
                            val installId = InstallSessionGuard.currentInstallId(context)
                            scope.launch(Dispatchers.IO) {
                                AppPreferences.marcarUbicacionSolicitada(context, installId)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextButton(
                        onClick = { mostrarAviso = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Ahora no", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Abrir ajustes", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUbicacionPermisoGate() {
    PikuTheme {
        UbicacionPermisoGate {
            Text("Contenido del mapa", modifier = Modifier.padding(24.dp))
        }
    }
}
