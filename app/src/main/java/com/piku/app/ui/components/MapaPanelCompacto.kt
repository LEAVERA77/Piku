package com.piku.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.preview.PreviewMocks
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.model.Rubro

@Composable
fun MapaPanelCompacto(
    busquedaNombre: String,
    busquedaDireccion: String,
    contadorVisibles: Int,
    rubros: List<Rubro>,
    rubrosSeleccionados: Set<String>,
    expandido: Boolean,
    onToggleExpandido: () -> Unit,
    onBusquedaNombreChange: (String) -> Unit,
    onBusquedaDireccionChange: (String) -> Unit,
    onIrDireccion: () -> Unit,
    onToggleRubro: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        tonalElevation = 2.dp
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = busquedaNombre,
                    onValueChange = onBusquedaNombreChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Comercios") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, null, Modifier.size(20.dp))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                IconButton(onClick = onToggleExpandido, modifier = Modifier.size(36.dp)) {
                    Icon(
                        if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Dirección"
                    )
                }
            }
            if (expandido) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = busquedaDireccion,
                        onValueChange = onBusquedaDireccionChange,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Calle y número") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        textStyle = MaterialTheme.typography.bodySmall,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { onIrDireccion() })
                    )
                    TextButton(
                        onClick = onIrDireccion,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            "Ir",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                "$contadorVisibles comercios",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (rubros.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rubros.forEach { rubro ->
                        val selected = rubrosSeleccionados.contains(rubro.id)
                        FilterChip(
                            selected = selected,
                            onClick = { onToggleRubro(rubro.id) },
                            label = { Text(rubro.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMapaPanelCompacto() {
    PikuTheme {
        MapaPanelCompacto(
            busquedaNombre = "café",
            busquedaDireccion = "Corrientes 1200",
            contadorVisibles = 5,
            rubros = PreviewMocks.rubros,
            rubrosSeleccionados = setOf("cafeteria"),
            expandido = true,
            onToggleExpandido = {},
            onBusquedaNombreChange = {},
            onBusquedaDireccionChange = {},
            onIrDireccion = {},
            onToggleRubro = {}
        )
    }
}
