package com.piku.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.piku.app.data.model.RecompensaPublica
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku

@Composable
fun ArticuloCatalogoRow(
    articulo: RecompensaPublica,
    photoUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cantidadFotos: Int = 1
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            PikuPhotoImage(
                url = photoUrl,
                contentDescription = articulo.nombre,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                cornerRadius = 12.dp,
                contentScale = ContentScale.Crop
            )
            IndicadorFotosBadge(
                cantidad = cantidadFotos,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(articulo.nombre, style = MaterialTheme.typography.titleMedium)
            val beneficio = articulo.resumenBeneficio()
            if (beneficio.isNotBlank()) {
                Text(
                    beneficio,
                    style = MaterialTheme.typography.labelLarge,
                    color = NaranjaPiku
                )
            }
            articulo.descripcion?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            Text(
                "${articulo.puntosRequeridos} PP para canjear",
                style = MaterialTheme.typography.bodySmall,
                color = VerdePiku
            )
        }
    }
}
