package com.piku.app.ui.screens

import android.Manifest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.piku.app.R
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.components.QrCameraPreview
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.theme.AcentoVerdeClaro
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.EscanerViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EscanerScreen(
    modifier: Modifier = Modifier,
    viewModel: EscanerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val permisoCamara = rememberPermissionState(Manifest.permission.CAMERA)
    val permisosUbicacion = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val puedeEscanear = permisoCamara.status.isGranted && permisosUbicacion.allPermissionsGranted

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Escanear QR",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(20.dp)
        )

        when {
            puedeEscanear -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    QrCameraPreview(
                        linternaActiva = uiState.linternaActiva,
                        escaneoHabilitado = uiState.escaneando && !uiState.validando,
                        onCodigoDetectado = viewModel::onCodigoEscaneado
                    )

                    // Marco cuadrado para encuadrar el QR
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(260.dp)
                            .border(
                                width = 3.dp,
                                color = if (uiState.escaneoExitoso) AcentoVerdeClaro else Color.White,
                                shape = RoundedCornerShape(20.dp)
                            )
                    )

                    if (uiState.validando) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = VerdePiku
                        )
                    } else if (uiState.escaneando) {
                        IndicadorEscaneando(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = { viewModel.alternarLinterna() },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Icon(
                            imageVector = if (uiState.linternaActiva) {
                                Icons.Default.FlashOn
                            } else {
                                Icons.Default.FlashOff
                            },
                            contentDescription = "Linterna",
                            tint = VerdePiku
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = uiState.mensaje,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        uiState.comercioNombre?.let { nombre ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = nombre,
                                style = MaterialTheme.typography.titleSmall,
                                color = VerdePiku,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        uiState.puntosGanados?.let { pts ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+$pts puntos",
                                style = MaterialTheme.typography.headlineSmall,
                                color = VerdePiku,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        uiState.saldoActual?.let { saldo ->
                            Text(
                                text = "Saldo: $saldo pts",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        uiState.error?.let { err ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                if (uiState.escaneoExitoso) {
                    BotonPiku(
                        texto = "Escanear otro",
                        onClick = { viewModel.reiniciarEscaneo() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        estilo = EstiloBotonPiku.SECUNDARIO
                    )
                } else {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            else -> {
                val faltaCamara = !permisoCamara.status.isGranted
                val faltaUbicacion = !permisosUbicacion.allPermissionsGranted
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    PikuPhotoImage(
                        url = PikuImages.permisoCamara,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .padding(bottom = 16.dp),
                        cornerRadius = 16.dp
                    )
                    Text(
                        text = when {
                            faltaCamara && faltaUbicacion ->
                                "Necesitamos cámara y ubicación para validar que estés en el comercio."
                            faltaCamara -> stringResource(R.string.permiso_camara)
                            else -> "Activá la ubicación para validar el QR en el comercio."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    BotonPiku(
                        texto = stringResource(R.string.conceder_permiso),
                        onClick = {
                            if (faltaCamara) permisoCamara.launchPermissionRequest()
                            else if (faltaUbicacion) permisosUbicacion.launchMultiplePermissionRequest()
                        },
                        estilo = EstiloBotonPiku.PRIMARIO
                    )
                }
            }
        }
    }
}

@Composable
private fun IndicadorEscaneando(modifier: Modifier = Modifier) {
    val transicion = rememberInfiniteTransition(label = "pulse")
    val alpha by transicion.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = modifier.alpha(alpha),
        colors = CardDefaults.cardColors(containerColor = VerdePiku.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "Escaneando…",
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewEscanerScreen() {
    PikuTheme {
        EscanerScreen()
    }
}
