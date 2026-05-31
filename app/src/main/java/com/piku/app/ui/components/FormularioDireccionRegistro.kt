package com.piku.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.piku.app.data.ProvinciasArgentina

data class DireccionFormState(
    val calle: String = "",
    val numero: String = "",
    val ciudad: String = "",
    val provincia: String = "",
    val codigoPostal: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormularioDireccionRegistro(
    state: DireccionFormState,
    onChange: (DireccionFormState) -> Unit,
    fieldColors: androidx.compose.material3.TextFieldColors,
    fieldShape: androidx.compose.foundation.shape.RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    var provinciaExpanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = state.calle,
        onValueChange = { onChange(state.copy(calle = it)) },
        label = { Text("Calle *") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = fieldShape,
        colors = fieldColors
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = state.numero,
        onValueChange = { onChange(state.copy(numero = it)) },
        label = { Text("Número *") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = fieldShape,
        colors = fieldColors
    )
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = state.ciudad,
        onValueChange = { onChange(state.copy(ciudad = it)) },
        label = { Text("Ciudad *") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = fieldShape,
        colors = fieldColors
    )
    Spacer(Modifier.height(10.dp))
    ExposedDropdownMenuBox(
        expanded = provinciaExpanded,
        onExpandedChange = { provinciaExpanded = it }
    ) {
        OutlinedTextField(
            value = state.provincia,
            onValueChange = {},
            readOnly = true,
            label = { Text("Provincia *") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = provinciaExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            shape = fieldShape,
            colors = fieldColors
        )
        ExposedDropdownMenu(
            expanded = provinciaExpanded,
            onDismissRequest = { provinciaExpanded = false }
        ) {
            ProvinciasArgentina.todas.forEach { prov ->
                DropdownMenuItem(
                    text = { Text(prov) },
                    onClick = {
                        onChange(state.copy(provincia = prov))
                        provinciaExpanded = false
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = state.codigoPostal,
        onValueChange = { onChange(state.copy(codigoPostal = it)) },
        label = { Text("Código postal (opcional)") },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = fieldShape,
        colors = fieldColors
    )
    Text(
        "La dirección se usa para ubicarte en el mapa.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
}
