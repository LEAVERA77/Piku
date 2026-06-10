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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
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
import com.piku.app.utils.CompartirLogro

@Composable
fun SaldoScreen(
    onEscanearClick: () -> Unit,
    onCanjearClick: () -> Unit,
    onRankingClick: () -> Unit = {},
    onDesafiosClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SaldoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Refresca cada vez que se vuelve a esta pestaña (no solo al primer montaje),
    // así el saldo queda al día después de escanear o canjear.
    LifecycleResumeEffect(Unit) {
        viewModel.refrescar()
        onPauseOrDispose { }
    }

    uiState.hitoParaCompartir?.let { hito ->
        AlertDialog(
            onDismissRequest = { viewModel.descartarHito() },
            title = { Text("¡Nuevo logro!") },
            text = { Text(CompartirLogro.mensajeHito(hito)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        CompartirLogro.compartirLogro(context, CompartirLogro.mensajeHito(hito))
                        viewModel.marcarHitoCompartido(hito)
                    }
                ) {
                    Text("Compartir")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.marcarHitoCompartido(hito) }) {
                    Text("Ahora no")
                }
            }
        )
    }

    if (uiState.cargando && uiState.transacciones.isEmpty() && uiState.puntos == 0) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
            uiState.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            AnimatedVisibility(visible = true, enter = fadeIn()) {
                TarjetaSaldo(
                    puntos = uiState.puntos,
                    equivalenciaDescuentoArs = uiState.equivalenciaDescuentoArs
                )
            }
            uiState.mensajeSaldo?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            uiState.mensajeInfo?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        if (uiState.desgloseDisponible) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("💰 Desglose de Piku Points", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            Text("Compras: +${uiState.puntosCompras} PP")
                            Spacer(Modifier.weight(1f))
                            Text("Canjes: -${uiState.puntosCanjes} PP")
                        }
                        Row(Modifier.fillMaxWidth()) {
                            Text("Bonos: +${uiState.puntosBonos} PP")
                            Spacer(Modifier.weight(1f))
                            Text("Saldo: ${uiState.puntos} PP")
                        }
                    }
                }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BotonPiku(
                    texto = "RANKING",
                    onClick = onRankingClick,
                    modifier = Modifier.weight(1f),
                    estilo = EstiloBotonPiku.CONTORNO,
                    icono = Icons.Default.Leaderboard
                )
                BotonPiku(
                    texto = "DESAFÍOS",
                    onClick = onDesafiosClick,
                    modifier = Modifier.weight(1f),
                    estilo = EstiloBotonPiku.CONTORNO,
                    icono = Icons.Default.EmojiEvents
                )
            }
        }

        item {
            BotonPiku(
                texto = "COMPARTIR MI SALDO",
                onClick = {
                    CompartirLogro.compartirLogro(
                        context,
                        CompartirLogro.mensajeSaldo(uiState.puntos, uiState.equivalenciaDescuentoArs)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                estilo = EstiloBotonPiku.CONTORNO,
                icono = Icons.Default.Share
            )
        }

        item {
            BotonPiku(
                texto = "COMPARTIR PIKU (+20 PTS)",
                onClick = {
                    viewModel.compartirPiku {
                        CompartirLogro.compartirLogro(
                            context,
                            "Sumá puntos y canjeá ofertas con Piku en comercios de tu ciudad."
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                estilo = EstiloBotonPiku.CONTORNO,
                icono = Icons.Default.Share
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
                text = "$signo${transaccion.puntos} PP",
                style = MaterialTheme.typography.titleMedium,
                color = colorPuntos
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSaldoScreen() {
    PikuTheme {
        SaldoScreen(onEscanearClick = {}, onCanjearClick = {})
    }
}
