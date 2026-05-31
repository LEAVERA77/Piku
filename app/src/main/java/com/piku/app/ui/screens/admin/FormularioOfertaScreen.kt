package com.piku.app.ui.screens.admin

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.model.OfertaComercio
import com.piku.app.data.repository.OfertaRepository
import coil.compose.AsyncImage
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.components.GaleriaArticuloEditor
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.media.PikuImages
import kotlinx.coroutines.launch
import java.util.Calendar

private val TIPOS = listOf(
    "producto_gratis" to "Producto gratis",
    "descuento" to "Descuento %",
    "2x1" to "2x1",
    "envio_gratis" to "Envío gratis"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FormularioOfertaScreen(
    ofertaId: String?,
    onBack: () -> Unit,
    onGuardado: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { OfertaRepository(context) }
    val esEdicion = !ofertaId.isNullOrBlank() && ofertaId != "new"

    var titulo by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var puntos by remember { mutableStateOf("100") }
    var tipo by remember { mutableStateOf("producto_gratis") }
    var porcentaje by remember { mutableStateOf("") }
    var productoNombre by remember { mutableStateOf("") }
    var fechaInicio by remember { mutableStateOf(fechaIsoHoy()) }
    var fechaFin by remember { mutableStateOf(fechaIsoEnDias(90)) }
    var maxPorUsuario by remember { mutableStateOf("1") }
    var maxTotales by remember { mutableStateOf("0") }
    var imagenUrl by remember { mutableStateOf<String?>(null) }
    var imagenLocal by remember { mutableStateOf<Uri?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    var idArticulo by remember { mutableStateOf(ofertaId?.takeIf { it != "new" }) }
    var ofertaActual by remember { mutableStateOf<OfertaComercio?>(null) }
    var listoParaSalir by remember { mutableStateOf(esEdicion) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) imagenLocal = uri }

    LaunchedEffect(ofertaId) {
        if (esEdicion && ofertaId != null) {
            try {
                val o = repo.obtener(ofertaId)
                titulo = o.nombre
                descripcion = o.descripcion.orEmpty()
                puntos = o.puntosRequeridos.toString()
                tipo = o.tipo ?: "producto_gratis"
                porcentaje = o.porcentajeDescuento?.toString().orEmpty()
                productoNombre = o.productoNombre.orEmpty()
                fechaInicio = o.fechaInicio?.take(10) ?: fechaIsoHoy()
                fechaFin = o.fechaFin?.take(10) ?: fechaIsoEnDias(90)
                maxPorUsuario = (o.maxUsosPorUsuario ?: 1).toString()
                maxTotales = (o.maxUsosTotales ?: 0).toString()
                imagenUrl = o.imagenUrl
                ofertaActual = o
                idArticulo = ofertaId
            } catch (e: Exception) {
                error = e.message
            }
        }
    }

    fun guardar() {
        scope.launch {
            cargando = true
            error = null
            try {
                val body = mutableMapOf<String, Any?>(
                    "nombre" to titulo.trim(),
                    "descripcion" to descripcion.trim(),
                    "puntosRequeridos" to (puntos.toIntOrNull() ?: 100),
                    "tipo" to tipo,
                    "fechaInicio" to "${fechaInicio}T00:00:00.000Z",
                    "fechaFin" to "${fechaFin}T23:59:59.000Z",
                    "maxUsosPorUsuario" to (maxPorUsuario.toIntOrNull() ?: 1),
                    "maxUsosTotales" to (maxTotales.toIntOrNull() ?: 0)
                )
                if (tipo == "descuento" && porcentaje.isNotBlank()) {
                    body["porcentajeDescuento"] = porcentaje.toIntOrNull()
                }
                if (tipo == "producto_gratis" && productoNombre.isNotBlank()) {
                    body["productoNombre"] = productoNombre.trim()
                }
                if (!imagenUrl.isNullOrBlank()) body["imagenUrl"] = imagenUrl

                val idExistente = idArticulo
                val id = if (esEdicion && idExistente != null) {
                    repo.actualizar(idExistente, body).id
                    idExistente
                } else {
                    repo.crear(body).id
                }
                idArticulo = id

                imagenLocal?.let { uri ->
                    val res = repo.subirImagen(id, uri)
                    imagenUrl = res.imagenUrl
                }
                ofertaActual = repo.obtener(id)
                listoParaSalir = true
            } catch (e: Exception) {
                error = e.message
            } finally {
                cargando = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (esEdicion) "Editar artículo" else "Publicar artículo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (imagenLocal != null) {
                AsyncImage(
                    model = imagenLocal,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                PikuPhotoImage(
                    url = imagenUrl ?: PikuImages.regalo,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                    cornerRadius = 16.dp
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Subir foto portada")
            }
            Spacer(Modifier.height(12.dp))
            GaleriaArticuloEditor(
                ofertaId = idArticulo,
                oferta = ofertaActual,
                onOfertaActualizada = {
                    ofertaActual = it
                    imagenUrl = it.imagenUrl
                }
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(titulo, { titulo = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                descripcion, { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(puntos, { puntos = it }, label = { Text("Puntos necesarios") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))
            Text("Tipo de oferta", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TIPOS.forEach { (k, label) ->
                    FilterChip(
                        selected = tipo == k,
                        onClick = { tipo = k },
                        label = { Text(label) }
                    )
                }
            }

            if (tipo == "descuento") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(porcentaje, { porcentaje = it }, label = { Text("Porcentaje de descuento") }, modifier = Modifier.fillMaxWidth())
            }
            if (tipo == "producto_gratis") {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(productoNombre, { productoNombre = it }, label = { Text("Producto (ej. Café mediano)") }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(fechaInicio, { fechaInicio = it }, label = { Text("Inicio (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(fechaFin, { fechaFin = it }, label = { Text("Fin (AAAA-MM-DD)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(maxPorUsuario, { maxPorUsuario = it }, label = { Text("Máx. usos por cliente (0=ilimitado)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(maxTotales, { maxTotales = it }, label = { Text("Máx. canjes totales (0=ilimitado)") }, modifier = Modifier.fillMaxWidth())

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            BotonPiku(
                texto = if (cargando) "Guardando…" else "Guardar artículo",
                onClick = {
                    if (titulo.isBlank()) error = "El título es obligatorio"
                    else guardar()
                },
                modifier = Modifier.fillMaxWidth(),
                habilitado = !cargando
            )
            if (listoParaSalir) {
                Spacer(Modifier.height(8.dp))
                BotonPiku(
                    texto = "Volver al catálogo",
                    onClick = onGuardado,
                    modifier = Modifier.fillMaxWidth(),
                    estilo = EstiloBotonPiku.CONTORNO,
                    habilitado = !cargando
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewNuevaOfertaScreen() {
    PikuTheme {
        FormularioOfertaScreen(
            ofertaId = "new",
            onBack = {},
            onGuardado = {}
        )
    }
}

private fun fechaIsoHoy(): String {
    val c = Calendar.getInstance()
    return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

private fun fechaIsoEnDias(dias: Int): String {
    val c = Calendar.getInstance()
    c.add(Calendar.DAY_OF_YEAR, dias)
    return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}
