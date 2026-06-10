package com.piku.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.viewmodel.DesafiosUiState
import com.piku.app.data.model.DesafioItem
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.DesafiosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesafiosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DesafiosViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) { viewModel.refrescar() }

    LaunchedEffect(uiState.mensajeExito) {
        uiState.mensajeExito?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Desafíos semanales") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        DesafiosContent(
            uiState = uiState,
            onCompletar = viewModel::completar,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
internal fun DesafiosContent(
    uiState: DesafiosUiState,
    onCompletar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.cargando && uiState.desafios.isEmpty() -> {
            Box(Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdePiku)
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Completá desafíos y sumá Piku Points extra esta semana.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                uiState.error?.let { err ->
                    item {
                        Text(err, color = MaterialTheme.colorScheme.error)
                    }
                }
                items(uiState.desafios, key = { it.id }) { desafio ->
                    DesafioCard(
                        desafio = desafio,
                        reclamando = uiState.reclamandoId == desafio.id,
                        habilitado = uiState.reclamandoId == null,
                        onCompletar = { onCompletar(desafio.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun DesafioCard(
    desafio: DesafioItem,
    onCompletar: () -> Unit,
    reclamando: Boolean = false,
    habilitado: Boolean = true
) {
    val progresoFrac = if (desafio.objetivo > 0) {
        (desafio.progreso.toFloat() / desafio.objetivo).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(desafio.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("+${desafio.recompensa} PP", color = VerdePiku, fontWeight = FontWeight.Bold)
            }
            desafio.descripcion?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(
                progress = { progresoFrac },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = VerdePiku,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                "${desafio.progreso} / ${desafio.objetivo}",
                style = MaterialTheme.typography.labelMedium
            )
            when {
                desafio.completado -> {
                    Text("✅ Completado", color = VerdePiku, fontWeight = FontWeight.SemiBold)
                }
                desafio.listoParaCompletar -> {
                    BotonPiku(
                        texto = if (reclamando) "RECLAMANDO…" else "RECLAMAR RECOMPENSA",
                        onClick = onCompletar,
                        modifier = Modifier.fillMaxWidth(),
                        habilitado = habilitado && !reclamando,
                        estilo = EstiloBotonPiku.PRIMARIO
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewDesafiosScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Desafíos semanales") })
            }
        ) { padding ->
            DesafiosContent(
                uiState = PreviewMocks.desafiosUiState,
                onCompletar = {},
                modifier = Modifier.padding(padding)
            )
        }
    }
}
