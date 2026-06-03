package com.piku.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.viewmodel.RankingUiState
import com.piku.app.data.TipoComercio
import com.piku.app.data.model.RankingComercioItem
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.RankingViewModel

private fun medalla(posicion: Int): String = when (posicion) {
    1 -> "🥇"
    2 -> "🥈"
    3 -> "🥉"
    else -> ""
}

private fun emojiRubro(rubro: String): String =
    TipoComercio.desdeId(rubro).emoji

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RankingComerciosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RankingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.refrescar() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Comercios populares") },
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
        RankingComerciosContent(uiState = uiState, modifier = Modifier.padding(padding))
    }
}

@Composable
internal fun RankingComerciosContent(
    uiState: RankingUiState,
    modifier: Modifier = Modifier
) {
    when {
        uiState.cargando && uiState.ranking.isEmpty() -> {
            Box(Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = VerdePiku)
            }
        }
        uiState.error != null -> {
            Box(Modifier.fillMaxSize().then(modifier), contentAlignment = Alignment.Center) {
                Text(uiState.error ?: "", color = MaterialTheme.colorScheme.error)
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
                        "Top canjes — ${uiState.mes}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                if (uiState.ranking.isEmpty()) {
                    item {
                        Text(
                            "Todavía no hay canjes este mes. ¡Sé el primero!",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(uiState.ranking, key = { it.posicion }) { item ->
                        RankingItemCard(item)
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun RankingItemCard(item: RankingComercioItem) {
    val esTop3 = item.posicion <= 3
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esTop3) {
                NaranjaPiku.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (medalla(item.posicion).isNotEmpty()) {
                    medalla(item.posicion)
                } else {
                    "#${item.posicion}"
                },
                fontSize = if (esTop3) 32.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.size(width = 48.dp, height = 48.dp)
            )
            Text(
                text = emojiRubro(item.rubro),
                fontSize = 28.sp
            )
            Column(Modifier.weight(1f)) {
                Text(item.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    TipoComercio.desdeId(item.rubro).etiqueta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${item.canjes}", fontWeight = FontWeight.Bold, color = VerdePiku, fontSize = 22.sp)
                Text("canjes", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun PreviewRankingComerciosScreen() {
    PikuTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Comercios populares") })
            }
        ) { padding ->
            RankingComerciosContent(
                uiState = PreviewMocks.rankingUiState,
                modifier = Modifier.padding(padding)
            )
        }
    }
}
