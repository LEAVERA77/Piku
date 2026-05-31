package com.piku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.components.TarjetaRecompensa
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.CanjesViewModel
import kotlinx.coroutines.delay

@Composable
fun CanjesScreen(
    onVerOferta: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: CanjesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargar()
    }

    LaunchedEffect(uiState.mensajeExito) {
        if (uiState.mensajeExito != null) {
            delay(5000)
            viewModel.limpiarMensaje()
        }
    }

    if (uiState.cargando && uiState.recompensas.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Canjear puntos",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Tienes ${uiState.puntosDisponibles} pts disponibles",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        uiState.error?.let { err ->
            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
        }

        uiState.mensajeExito?.let { mensaje ->
            Snackbar(
                modifier = Modifier.fillMaxWidth(),
                action = {}
            ) {
                Text(mensaje)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (uiState.recompensas.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PikuPhotoImage(
                    url = PikuImages.emptyRecompensas,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp),
                    cornerRadius = 20.dp
                )
                Text(
                    text = "Pronto habrá nuevas recompensas para ti",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(24.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.recompensas) { recompensa ->
                    TarjetaRecompensa(
                        recompensa = recompensa,
                        puedeCanjear = uiState.puntosDisponibles >= recompensa.puntosRequeridos,
                        onCanjear = { viewModel.solicitarCanje(recompensa) },
                        modifier = Modifier.clickable { onVerOferta(recompensa.id) }
                    )
                }
            }
        }
    }

    if (uiState.mostrarConfirmacion && uiState.recompensaSeleccionada != null) {
        val recompensa = uiState.recompensaSeleccionada!!
        AlertDialog(
            onDismissRequest = { viewModel.cancelarCanje() },
            title = { Text("Confirmar canje") },
            text = {
                Text(
                    "¿Canjear \"${recompensa.nombre}\" por ${recompensa.puntosRequeridos} puntos?"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmarCanje() },
                    enabled = !uiState.canjeando,
                    colors = ButtonDefaults.buttonColors(containerColor = VerdePiku)
                ) {
                    Text(if (uiState.canjeando) "Canjeando…" else "Canjear")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.cancelarCanje() },
                    enabled = !uiState.canjeando
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCanjesScreen() {
    PikuTheme {
        CanjesScreen()
    }
}
