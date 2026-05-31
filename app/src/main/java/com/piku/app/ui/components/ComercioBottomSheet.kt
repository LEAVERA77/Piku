package com.piku.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piku.app.data.TipoComercio
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.RecompensaPublica
import com.piku.app.ui.theme.VerdePiku

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComercioBottomSheet(
    comercio: Comercio,
    ofertas: List<RecompensaPublica>,
    cargando: Boolean,
    onDismiss: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val context = LocalContext.current
    val cloud = ConfigLoader.cloudinaryCloudName(context)
    val emoji = TipoComercio.emojiPara(comercio)
    val tipoLabel = TipoComercio.desdeId(comercio.tipoComercio ?: comercio.categoria).etiqueta
    val envioTexto = comercio.textoEnvio()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            val tituloEmoji = if (comercio.realizaEnvios) "$emoji 🚲" else emoji
            Text(
                text = "$tituloEmoji  ${comercio.nombre}",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = tipoLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            envioTexto?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                comercio.telefonoContacto?.let { tel ->
                    Text(
                        text = "📞 Contacto: $tel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (comercio.cantidadOfertas > 0) {
                Text(
                    text = "🔥 ${comercio.cantidadOfertas} oferta(s) activa(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VerdePiku,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            comercio.direccion?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Ofertas", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            when {
                cargando -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                ofertas.isEmpty() -> Text(
                    "Sin ofertas activas por ahora.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn {
                    items(ofertas, key = { it.id }) { oferta ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVerDetalle(oferta.id) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PikuPhotoImage(
                                url = oferta.photoUrl(cloud),
                                contentDescription = oferta.nombre,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                cornerRadius = 10.dp,
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(oferta.nombre, style = MaterialTheme.typography.titleMedium)
                                oferta.tipo?.let { t ->
                                    Text(
                                        t.replace('_', ' '),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "${oferta.puntosRequeridos} pts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VerdePiku
                                )
                            }
                            Button(onClick = { onVerDetalle(oferta.id) }) {
                                Text("Ver")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
