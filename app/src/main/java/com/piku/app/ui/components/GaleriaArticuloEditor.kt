package com.piku.app.ui.components

import android.net.Uri
import com.piku.app.utils.rememberImagePicker
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.piku.app.data.model.OfertaComercio
import com.piku.app.data.model.RecompensaImagen
import com.piku.app.data.repository.OfertaRepository
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import kotlinx.coroutines.launch

@Composable
fun GaleriaArticuloEditor(
    ofertaId: String?,
    oferta: OfertaComercio?,
    onOfertaActualizada: (OfertaComercio) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { OfertaRepository(context) }

    var imagenes by remember { mutableStateOf<List<RecompensaImagen>>(emptyList()) }
    var portadaUrl by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf<String?>(null) }

    fun aplicarOferta(o: OfertaComercio) {
        portadaUrl = o.imagenUrl
        imagenes = o.imagenes.orEmpty()
        onOfertaActualizada(o)
    }

    LaunchedEffect(ofertaId, oferta) {
        if (oferta != null) aplicarOferta(oferta)
        if (ofertaId.isNullOrBlank() || ofertaId == "new") return@LaunchedEffect
        try {
            val res = repo.listarImagenesGaleria(ofertaId)
            portadaUrl = res.portadaUrl
            imagenes = res.imagenes
        } catch (_: Exception) {
            oferta?.let { aplicarOferta(it) }
        }
    }

    val elegirFoto = rememberImagePicker { uri ->
        if (ofertaId.isNullOrBlank() || ofertaId == "new") return@rememberImagePicker
        scope.launch {
            cargando = true
            mensaje = null
            try {
                val comoPortada = portadaUrl.isNullOrBlank() && imagenes.isEmpty()
                val o = repo.subirImagenGaleria(ofertaId, uri, comoPortada)
                aplicarOferta(o)
                mensaje = "Foto agregada"
            } catch (e: Exception) {
                mensaje = e.message
            } finally {
                cargando = false
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Galería de fotos", style = MaterialTheme.typography.titleSmall)
            Text(
                "La foto con estrella es la portada en el mapa.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))

            if (ofertaId.isNullOrBlank() || ofertaId == "new") {
                Text(
                    "Guardá el artículo primero para agregar varias fotos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            if (cargando) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(8.dp),
                    color = VerdePiku
                )
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val portada = portadaUrl ?: oferta?.imagenUrl
                if (!portada.isNullOrBlank()) {
                    item(key = "portada_$portada") {
                        FotoGaleriaItem(
                            url = portada,
                            esPortada = true,
                            onMarcarPortada = null,
                            onEliminar = null
                        )
                    }
                }
                items(imagenes, key = { it.id }) { img ->
                    if (img.imagenUrl != portada) {
                        FotoGaleriaItem(
                            url = img.imagenUrl,
                            esPortada = img.imagenUrl == portada,
                            onMarcarPortada = {
                                scope.launch {
                                    cargando = true
                                    try {
                                        aplicarOferta(repo.establecerPortada(ofertaId, img.id))
                                    } catch (e: Exception) {
                                        mensaje = e.message
                                    } finally {
                                        cargando = false
                                    }
                                }
                            },
                            onEliminar = {
                                scope.launch {
                                    cargando = true
                                    try {
                                        aplicarOferta(repo.eliminarImagenGaleria(ofertaId, img.id))
                                    } catch (e: Exception) {
                                        mensaje = e.message
                                    } finally {
                                        cargando = false
                                    }
                                }
                            }
                        )
                    }
                }
                item(key = "add") {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(2.dp, VerdePiku.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { elegirFoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar foto", tint = VerdePiku)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { elegirFoto() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !cargando
            ) {
                Text("Agregar otra foto")
            }
            mensaje?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = VerdePiku, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun FotoGaleriaItem(
    url: String,
    esPortada: Boolean,
    onMarcarPortada: (() -> Unit)?,
    onEliminar: (() -> Unit)?
) {
    Box {
        PikuPhotoImage(
            url = url,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp)),
            cornerRadius = 12.dp,
            contentScale = ContentScale.Crop
        )
        if (esPortada) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Portada",
                tint = NaranjaPiku,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
            )
        } else if (onMarcarPortada != null) {
            IconButton(
                onClick = onMarcarPortada,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Usar como portada",
                    tint = Color.White
                )
            }
        }
        onEliminar?.let { eliminar ->
            IconButton(
                onClick = eliminar,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White)
            }
        }
    }
}
