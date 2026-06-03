package com.piku.app.ui.screens.comercio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.viewmodel.NotificacionesComercioUiState
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.NotificacionesComercioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificacionesComercioScreen(
    onBack: () -> Unit,
    viewModel: NotificacionesComercioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        NotificacionesComercioContent(
            uiState = uiState,
            onMarcarLeida = viewModel::marcarLeida,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun NotificacionesComercioContent(
    uiState: NotificacionesComercioUiState,
    onMarcarLeida: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.cargando -> {
            CircularProgressIndicator(
                modifier = modifier
                    .fillMaxSize()
                    .padding(32.dp),
                color = VerdePiku
            )
        }
        uiState.error != null -> {
            Text(
                uiState.error!!,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(16.dp)
            )
        }
        uiState.notificaciones.isEmpty() -> {
            Text(
                "No tenés notificaciones.",
                modifier = modifier.padding(16.dp)
            )
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.notificaciones, key = { it.id }) { n ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !n.leida) {
                                onMarcarLeida(n.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (n.leida) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(n.titulo, style = MaterialTheme.typography.titleMedium)
                            Text(n.cuerpo, style = MaterialTheme.typography.bodyMedium)
                            n.createdAt?.let {
                                Text(it, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewNotificacionesComercioScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Notificaciones") })
            }
        ) { padding ->
            NotificacionesComercioContent(
                uiState = PreviewMocks.notificacionesUiState,
                onMarcarLeida = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
