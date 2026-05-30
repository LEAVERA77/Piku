package com.piku.app.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.piku.app.data.datastore.AppPreferences
import com.piku.app.data.security.InstallSessionGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PERMISOS_UBICACION = listOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

/**
 * Al abrir la app (nueva instalación), dispara el diálogo del sistema de ubicación.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SolicitudUbicacionAlInicio() {
    val context = LocalContext.current
    val permisos = rememberMultiplePermissionsState(PERMISOS_UBICACION)

    LaunchedEffect(permisos.allPermissionsGranted) {
        if (permisos.allPermissionsGranted) return@LaunchedEffect
        val installId = InstallSessionGuard.currentInstallId(context)
        val debePedir = withContext(Dispatchers.IO) {
            AppPreferences.debeSolicitarUbicacion(context, installId)
        }
        if (debePedir) {
            permisos.launchMultiplePermissionRequest()
            withContext(Dispatchers.IO) {
                AppPreferences.marcarUbicacionSolicitada(context, installId)
            }
        }
    }
}

/**
 * Envuelve el flujo cliente: si no hay permiso, muestra aviso minimalista para activarlo.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun UbicacionPermisoGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val permisos = rememberMultiplePermissionsState(PERMISOS_UBICACION)
    val concedido = permisos.allPermissionsGranted

    LaunchedEffect(concedido) {
        if (!concedido) {
            val installId = InstallSessionGuard.currentInstallId(context)
            val debePedir = withContext(Dispatchers.IO) {
                AppPreferences.debeSolicitarUbicacion(context, installId)
            }
            if (debePedir) {
                permisos.launchMultiplePermissionRequest()
                withContext(Dispatchers.IO) {
                    AppPreferences.marcarUbicacionSolicitada(context, installId)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        content()
        if (!concedido) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Ubicación para Piku",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Necesitamos tu ubicación para mostrarte ofertas y comercios cerca de vos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Start
                        )
                        BotonPiku(
                            texto = "Permitir ubicación",
                            onClick = { permisos.launchMultiplePermissionRequest() },
                            modifier = Modifier.fillMaxWidth()
                        )
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
                            Text("Abrir ajustes")
                        }
                    }
                }
            }
        }
    }
}
