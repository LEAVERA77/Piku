package com.piku.app.ui.screens.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.utils.QrShareHelper
import com.piku.app.ui.viewmodel.AdminGenerarQrViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGenerarQrScreen(
    onBack: () -> Unit,
    viewModel: AdminGenerarQrViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Generar QR") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Ingresá el monto de la compra. El cliente escanea el QR para sumar puntos.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = uiState.monto,
                onValueChange = viewModel::onMontoChange,
                label = { Text("Monto de la compra") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.cargando && uiState.qr == null
            )
            if (uiState.qr == null) {
                BotonPiku(
                    texto = if (uiState.cargando) "Generando…" else "Generar QR",
                    onClick = { if (!uiState.cargando) viewModel.generar() },
                    modifier = Modifier.fillMaxWidth(),
                    habilitado = !uiState.cargando && !uiState.limiteAlcanzado
                )
                if (uiState.limiteAlcanzado) {
                    Text(
                        "Límite mensual de puntos alcanzado. Actualizá tu plan en Suscripción.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            } else {
                BotonPiku(
                    texto = "Generar otro",
                    onClick = viewModel::limpiar,
                    modifier = Modifier.fillMaxWidth(),
                    estilo = EstiloBotonPiku.SECUNDARIO
                )
            }
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
            if (uiState.cargando) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = VerdePiku
                )
            }
            uiState.qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Código QR para el cliente",
                    modifier = Modifier
                        .size(280.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
            uiState.qr?.let { qr ->
                Text(
                    "Código: ${qr.codigo}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                qr.puntosCalculados?.let { pts ->
                    Text(
                        "Piku Points a acreditar: $pts PP",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = VerdePiku
                    )
                }
                uiState.expiraEnMinutos?.let { min ->
                    Text(
                        "Válido ${min} minutos",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                qr.expiraAt?.let { exp ->
                    Text(
                        "Expira: $exp",
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                uiState.qrBitmap?.let { bitmap ->
                    val monto = uiState.monto.toDoubleOrNull() ?: 0.0
                    BotonPiku(
                        texto = "Compartir por WhatsApp",
                        onClick = {
                            QrShareHelper.compartirQr(
                                context = context,
                                bitmap = bitmap,
                                codigo = qr.codigo,
                                monto = monto,
                                puntos = qr.puntosCalculados,
                                minutosValidez = uiState.expiraEnMinutos
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icono = Icons.Default.Share
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGenerarQRScreen() {
    PikuTheme {
        AdminGenerarQrScreen(onBack = {})
    }
}
