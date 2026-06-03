package com.piku.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piku.app.data.TipoComercio
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.RecompensaPublica

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComercioBottomSheet(
    comercio: Comercio,
    ofertas: List<RecompensaPublica>,
    cargando: Boolean,
    onDismiss: () -> Unit,
    onVerCatalogo: () -> Unit,
    onVerDetalle: (String) -> Unit
) {
    val emoji = TipoComercio.emojiPara(comercio)
    val tipoLabel = TipoComercio.desdeId(comercio.tipoComercio ?: comercio.categoria).etiqueta
    val envioTexto = comercio.textoEnvio()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 32.sp)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        buildString {
                            if (comercio.destacado && !comercio.esOpenStreetMap()) append("⭐ ")
                            append(comercio.nombre)
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        buildString {
                            append(tipoLabel)
                            if (comercio.realizaEnvios) append(" · 🚲 Envíos disponibles")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            envioTexto?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
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

            if (comercio.realizaEnvios || !comercio.telefonoContacto.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                ComercioEnvioContactoCard(comercio = comercio)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "🎁 Ofertas vigentes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            when {
                cargando -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                ofertas.isEmpty() -> Text(
                    "No hay ofertas activas por el momento",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> {
                    val maxH = maxOf(120, ofertas.size.coerceAtMost(4) * 132).dp
                    LazyColumn(modifier = Modifier.heightIn(max = maxH)) {
                        items(ofertas, key = { it.id }) { oferta ->
                            OfertaCard(
                                oferta = oferta,
                                onVerMas = { onVerDetalle(oferta.id) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            if (!comercio.esDemo()) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onVerCatalogo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver catálogo completo")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
