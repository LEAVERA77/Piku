package com.piku.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piku.app.data.TipoComercio
import com.piku.app.data.config.ConfigLoader
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
            }
            if (comercio.cantidadOfertas > 0) {
                Text(
                    text = "🔥 ${comercio.cantidadOfertas} oferta(s) activa(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
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
            if (comercio.realizaEnvios || !comercio.telefonoContacto.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                ComercioEnvioContactoCard(comercio = comercio)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onVerCatalogo,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Ver todos los artículos")
            }
            Spacer(Modifier.height(12.dp))
            Text("Artículos publicados", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            when {
                cargando -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                ofertas.isEmpty() -> Text(
                    "Este comercio aún no publicó artículos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn {
                    items(ofertas, key = { it.id }) { oferta ->
                        ArticuloCatalogoRow(
                            articulo = oferta,
                            photoUrl = oferta.photoUrl(cloud),
                            cantidadFotos = oferta.todasLasFotos(cloud).size,
                            onClick = { onVerDetalle(oferta.id) }
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
