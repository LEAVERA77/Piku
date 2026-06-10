package com.piku.app.ui.screens.comercio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.viewmodel.HistorialCanjesUiState
import com.piku.app.data.model.CanjeComercioItem
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.HistorialCanjesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialCanjesScreen(
    onBack: () -> Unit,
    viewModel: HistorialCanjesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val finLista by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= uiState.canjes.size - 3
        }
    }

    LaunchedEffect(finLista) {
        if (finLista && uiState.hayMas && !uiState.cargando && !uiState.cargandoMas) {
            viewModel.cargarMas()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de canjes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        HistorialCanjesContent(
            uiState = uiState,
            onBuscarChange = viewModel::onBuscarChange,
            onAplicarBusqueda = viewModel::aplicarBusqueda,
            listState = listState,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun HistorialCanjesContent(
    uiState: HistorialCanjesUiState,
    onBuscarChange: (String) -> Unit,
    onAplicarBusqueda: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    Column(
        Modifier
            .fillMaxSize()
            .then(modifier)
    ) {
        OutlinedTextField(
            value = uiState.buscar,
            onValueChange = onBuscarChange,
            label = { Text("Buscar cliente u oferta") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        BotonPiku(
            texto = "Buscar",
            onClick = onAplicarBusqueda,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        when {
            uiState.cargando && uiState.canjes.isEmpty() -> {
                CircularProgressIndicator(
                    modifier = Modifier.padding(32.dp),
                    color = VerdePiku
                )
            }
            uiState.error != null -> {
                Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
                BotonPiku(
                    texto = "Reintentar",
                    onClick = onAplicarBusqueda,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            uiState.canjes.isEmpty() -> {
                Text(
                    "No hay canjes todavía.",
                    modifier = Modifier.padding(16.dp)
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.canjes, key = { it.id }) { canje ->
                        TarjetaCanjeComercio(canje)
                    }
                    if (uiState.cargandoMas) {
                        item {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(16.dp),
                                color = VerdePiku
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaCanjeComercio(canje: CanjeComercioItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(canje.ofertaNombre ?: "Oferta", style = MaterialTheme.typography.titleMedium)
            Text(
                canje.clienteNombre ?: "Cliente",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${canje.puntosUsados} pts · ${canje.estado}",
                style = MaterialTheme.typography.bodySmall,
                color = VerdePiku
            )
            Text(
                "Código: ${canje.codigoCanje}",
                style = MaterialTheme.typography.labelMedium
            )
            canje.createdAt?.let {
                Text(it, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewHistorialCanjesScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Historial de canjes") })
            }
        ) { padding ->
            HistorialCanjesContent(
                uiState = PreviewMocks.historialCanjesUiState,
                onBuscarChange = {},
                onAplicarBusqueda = {},
                listState = rememberLazyListState(),
                modifier = Modifier.padding(padding)
            )
        }
    }
}
