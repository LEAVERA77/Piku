package com.piku.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.Rubro
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.utils.DistanceCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkerInfoBottomSheet(
    comercio: Comercio,
    rubros: List<Rubro> = emptyList(),
    onDismiss: () -> Unit,
    onVerOfertas: () -> Unit
) {
    val rubroLabel = rubros.firstOrNull { rubro ->
        val cat = comercio.categoria?.lowercase()?.trim() ?: return@firstOrNull false
        rubro.categorias.any { c -> cat == c.lowercase() || cat.contains(c.lowercase()) }
    }?.label ?: comercio.categoria?.replaceFirstChar { it.uppercase() }
    val context = LocalContext.current
    val cloud = ConfigLoader.cloudinaryCloudName(context)
    val logoUrl = comercio.logoUrl
        ?: cloud?.let { "https://res.cloudinary.com/$it/image/upload/w_120,h_120,c_fill/piku/placeholder" }
        ?: PikuImages.comercioDefault

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PikuPhotoImage(
                url = logoUrl,
                contentDescription = comercio.nombre,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(16.dp)),
                cornerRadius = 16.dp,
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = comercio.nombre, style = MaterialTheme.typography.headlineSmall)
            rubroLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (comercio.esOpenStreetMap()) {
                Text(
                    text = "En OpenStreetMap · Aún no está en Piku",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                comercio.puntosMinCanje?.let { pts ->
                    Text(
                        text = "Canje desde $pts puntos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            comercio.direccion?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            comercio.distanciaMetros?.let {
                Text(
                    text = "A ${DistanceCalculator.formatoDistancia(it)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = VerdePiku
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (!comercio.esOpenStreetMap()) {
                Button(onClick = onVerOfertas, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver ofertas")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMarkerInfoBottomSheet() {
    PikuTheme {
        MarkerInfoBottomSheet(
            comercio = PreviewMocks.comercioPiku,
            rubros = PreviewMocks.rubros,
            onDismiss = {},
            onVerOfertas = {}
        )
    }
}
