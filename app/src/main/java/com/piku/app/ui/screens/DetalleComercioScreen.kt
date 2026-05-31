package com.piku.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.model.ComercioDetalleResponse
import com.piku.app.data.repository.ComercioRepository
import com.piku.app.data.repository.MapaRepository
import com.piku.app.ui.components.ArticuloCatalogoRow
import com.piku.app.ui.components.ComercioEnvioContactoCard
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.media.PikuImages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleComercioScreen(
    comercioId: String,
    onBack: () -> Unit,
    onVerArticulo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var detalle by remember { mutableStateOf<ComercioDetalleResponse?>(null) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(comercioId) {
        MapaRepository(context).registrarEvento("vista_comercio", comercioId)
        cargando = true
        try {
            detalle = ComercioRepository(context).detalleComercio(comercioId)
        } catch (e: Exception) {
            error = e.message
        } finally {
            cargando = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detalle?.comercio?.nombre ?: "Comercio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            cargando -> CircularProgressIndicator(Modifier.padding(padding).padding(32.dp))
            error != null -> Text(error!!, Modifier.padding(padding).padding(16.dp))
            detalle != null -> {
                val comercio = detalle!!.comercio
                val cloud = ConfigLoader.cloudinaryCloudName(context)
                val headerUrl = comercio.logoUrl
                    ?: cloud?.let { "https://res.cloudinary.com/$it/image/upload/w_800,h_300,c_fill/piku/placeholder" }
                    ?: PikuImages.comercioDefault

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    item {
                        PikuPhotoImage(
                            url = headerUrl,
                            contentDescription = comercio.nombre,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    item {
                        Column(Modifier.padding(16.dp)) {
                            Text(comercio.direccion ?: "", style = MaterialTheme.typography.bodyMedium)
                            if (comercio.realizaEnvios || !comercio.telefonoContacto.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                ComercioEnvioContactoCard(comercio = comercio)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Artículos publicados", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    items(detalle!!.recompensas, key = { it.id }) { r ->
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            ArticuloCatalogoRow(
                                articulo = r,
                                photoUrl = r.photoUrl(cloud),
                                onClick = { onVerArticulo(r.id) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewDetalleComercioScreen() {
    PikuTheme {
        DetalleComercioScreen(
            comercioId = PreviewMocks.comercioPiku.id,
            onBack = {}
        )
    }
}
