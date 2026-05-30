package com.piku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piku.app.ui.viewmodel.MensajeChat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharlaConPikuSheet(
    mensajes: List<MensajeChat>,
    pregunta: String,
    cargando: Boolean,
    comercioSugeridoId: String?,
    onPreguntaChange: (String) -> Unit,
    onEnviar: () -> Unit,
    onVerEnMapa: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text("Charla con Piku", style = MaterialTheme.typography.titleLarge)
            Text(
                "Te ayudo a elegir dónde canjear tus puntos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 300.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(mensajes) { msg ->
                    val esUsuario = msg.rol == "usuario"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (esUsuario) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            text = msg.texto,
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .background(
                                    if (esUsuario) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            if (comercioSugeridoId != null) {
                OutlinedButton(
                    onClick = onVerEnMapa,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Text("Ver en mapa")
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = pregunta,
                    onValueChange = onPreguntaChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("¿Qué me conviene cerca?") },
                    singleLine = true,
                    enabled = !cargando
                )
                Button(
                    onClick = onEnviar,
                    enabled = !cargando && pregunta.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(4.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Enviar")
                    }
                }
            }
        }
    }
}
