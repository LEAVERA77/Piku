package com.piku.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.ui.components.AvatarDisplay
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.viewmodel.PerfilViewModel

@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary
    )

    val pickerFoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) viewModel.subirFotoAvatar(uri)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
            if (!uiState.editando && !uiState.cargando) {
                TextButton(onClick = { viewModel.setEditando(true) }) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                    Text("Editar", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        if (uiState.cargando) {
            Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
        }
        uiState.mensajeExito?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
        }

        val perfil = uiState.perfil ?: return@Column

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            AvatarDisplay(
                avatarUrl = uiState.avatarUrl,
                modifier = Modifier.size(96.dp)
            )
            if (uiState.editando) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            pickerFoto.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Foto",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (uiState.editando) {
            Text(
                "Elegí un avatar",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.avataresEmoji.forEach { emoji ->
                    FilterChip(
                        selected = uiState.avatarUrl == "emoji:$emoji",
                        onClick = { viewModel.seleccionarAvatarEmoji(emoji) },
                        label = { Text(emoji, fontSize = 20.sp) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.editando) {
            CampoPerfil("Nombre", uiState.nombre, viewModel::onNombreChange, fieldColors)
            CampoPerfil("Teléfono", uiState.telefono, viewModel::onTelefonoChange, fieldColors)
            CampoPerfil("Dirección de envío", uiState.direccionEntrega, viewModel::onDireccionChange, fieldColors)
            CampoPerfil("Ciudad", uiState.ciudad, viewModel::onCiudadChange, fieldColors)
            CampoPerfil("Provincia", uiState.provincia, viewModel::onProvinciaChange, fieldColors)
            CampoPerfil("Código postal", uiState.codigoPostal, viewModel::onCodigoPostalChange, fieldColors)
            CampoPerfil("Notas para el reparto", uiState.notasEntrega, viewModel::onNotasChange, fieldColors, singleLine = false)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { viewModel.setEditando(false) },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar") }
                BotonPiku(
                    texto = if (uiState.guardando) "Guardando…" else "Guardar",
                    onClick = { viewModel.guardarPerfil() },
                    habilitado = !uiState.guardando,
                    estilo = EstiloBotonPiku.PRIMARIO,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(perfil.nombre, style = MaterialTheme.typography.titleLarge)
            Text(
                perfil.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${perfil.puntosSaldo} puntos · Nivel ${viewModel.nivelActual().etiqueta}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!perfil.telefono.isNullOrBlank()) {
                Text("Tel: ${perfil.telefono}", style = MaterialTheme.typography.bodySmall)
            }
            if (!perfil.direccionEntrega.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Envío a domicilio", style = MaterialTheme.typography.labelLarge)
                        Text(perfil.direccionEntrega, style = MaterialTheme.typography.bodyMedium)
                        listOfNotNull(perfil.ciudad, perfil.provincia, perfil.codigoPostal)
                            .filter { !it.isNullOrBlank() }
                            .joinToString(", ")
                            .takeIf { it.isNotEmpty() }
                            ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        perfil.notasEntrega?.let {
                            Text("Notas: $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { viewModel.cerrarSesion(onCerrarSesion) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.size(8.dp))
            Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
        }
        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun CampoPerfil(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    colors: androidx.compose.material3.TextFieldColors,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        singleLine = singleLine,
        shape = RoundedCornerShape(12.dp),
        colors = colors
    )
}
