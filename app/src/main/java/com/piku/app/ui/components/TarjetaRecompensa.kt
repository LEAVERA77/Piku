package com.piku.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.data.model.Recompensa
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.NaranjaPiku

@Composable
fun TarjetaRecompensa(
    recompensa: Recompensa,
    puedeCanjear: Boolean,
    onCanjear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PikuPhotoImage(
                url = recompensa.imageUrl,
                contentDescription = recompensa.nombre,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = recompensa.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = recompensa.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "${recompensa.puntosRequeridos} PP",
                    style = MaterialTheme.typography.labelLarge,
                    color = NaranjaPiku
                )
                Spacer(modifier = Modifier.height(8.dp))
                BotonPiku(
                    texto = "Canjear",
                    onClick = onCanjear,
                    modifier = Modifier.fillMaxWidth(),
                    estilo = EstiloBotonPiku.SECUNDARIO,
                    habilitado = puedeCanjear
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTarjetaRecompensa() {
    PikuTheme {
        TarjetaRecompensa(
            recompensa = PreviewMocks.recompensa,
            puedeCanjear = true,
            onCanjear = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
