package com.piku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.NaranjaPiku

@Composable
fun ArticuloFotoCarousel(
    fotos: List<String>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    altura: Int = 240
) {
    if (fotos.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(fotos, key = { it }) { url ->
                PikuPhotoImage(
                    url = url,
                    contentDescription = contentDescription,
                    modifier = Modifier
                        .width(300.dp)
                        .height(altura.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    cornerRadius = 16.dp,
                    contentScale = ContentScale.Crop
                )
            }
        }
        if (fotos.size > 1) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${fotos.size} fotos — deslizá para ver",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun IndicadorFotosBadge(cantidad: Int, modifier: Modifier = Modifier) {
    if (cantidad <= 1) return
    Box(
        modifier = modifier
            .background(NaranjaPiku.copy(alpha = 0.9f), CircleShape)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "📷 $cantidad",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}
