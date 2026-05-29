package com.piku.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.R
import com.piku.app.data.model.TipoTransaccion
import com.piku.app.data.model.Transaccion
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.components.PikuLogo
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.components.TarjetaSaldo
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.SaldoViewModel

@Composable
fun SaldoScreen(
    onEscanearClick: () -> Unit,
    onCanjearClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SaldoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PikuLogo(
                compact = true,
                showTagline = true,
                tagline = stringResource(R.string.app_tagline),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            AnimatedVisibility(visible = true, enter = fadeIn()) {
                TarjetaSaldo(
                    puntos = uiState.puntos,
                    equivalenciaDescuento = uiState.equivalenciaDescuento
                )
            }
        }

        item {
            BotonPiku(
                texto = stringResource(R.string.escanear_qr).uppercase(),
                onClick = onEscanearClick,
                modifier = Modifier.fillMaxWidth(),
                estilo = EstiloBotonPiku.PRIMARIO,
                icono = Icons.Default.QrCodeScanner
            )
        }

        item {
            BotonPiku(
                texto = stringResource(R.string.canjear_puntos).uppercase(),
                onClick = onCanjearClick,
                modifier = Modifier.fillMaxWidth(),
                estilo = EstiloBotonPiku.CONTORNO,
                icono = Icons.Default.CardGiftcard
            )
        }

        item {
            Text(
                text = "Últimos movimientos",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (uiState.transacciones.isEmpty()) {
            item {
                Text(
                    text = "Aún no hay movimientos. Escanea un QR para sumar puntos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.transacciones) { transaccion ->
                ItemTransaccion(transaccion)
            }
        }

        item { Spacer(modifier = Modifier.height(72.dp)) }
    }
}

@Composable
private fun ItemTransaccion(transaccion: Transaccion) {
    val esGanado = transaccion.tipo == TipoTransaccion.GANADO
    val colorPuntos = if (esGanado) VerdePiku else NaranjaPiku
    val signo = if (transaccion.puntos >= 0) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PikuPhotoImage(
                url = PikuImages.forTransaccion(transaccion.descripcion, transaccion.tipo),
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp)),
                cornerRadius = 12.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaccion.descripcion, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = transaccion.fecha,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "$signo${transaccion.puntos} pts",
                style = MaterialTheme.typography.titleMedium,
                color = colorPuntos
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SaldoScreenPreview() {
    PikuTheme {
        SaldoScreen(onEscanearClick = {}, onCanjearClick = {})
    }
}
