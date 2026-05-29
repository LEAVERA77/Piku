package com.piku.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.piku.app.data.model.NivelUsuario
import com.piku.app.ui.components.PikuPhotoImage
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku
import com.piku.app.ui.viewmodel.PerfilViewModel

@Composable
fun PerfilScreen(
    onCerrarSesion: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PerfilViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val usuario = uiState.usuario
    val nivel = viewModel.nivelActual()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!uiState.sesionActiva) {
            Text(
                text = "Sesión cerrada. Vuelve a iniciar sesión pronto.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
            return
        }

        PikuPhotoImage(
            url = PikuImages.avatarDefault,
            contentDescription = "Avatar",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape),
            cornerRadius = 50.dp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = usuario.nombre, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = usuario.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        BadgeNivel(nivel = nivel)

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tu código Piku",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "▣ ${usuario.codigoQr}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = VerdePiku
                )
                Text(
                    text = "Muéstralo en comercios afiliados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Puntos actuales", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "${usuario.puntos} pts",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NaranjaPiku
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = {
                viewModel.cerrarSesion()
                onCerrarSesion()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = NaranjaPiku)
                Spacer(modifier = Modifier.size(8.dp))
                Text("Cerrar sesión", color = NaranjaPiku)
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }
}

@Composable
private fun BadgeNivel(nivel: NivelUsuario) {
    val colorFondo = when (nivel) {
        NivelUsuario.BRONCE -> Color(0xFFCD7F32)
        NivelUsuario.PLATA -> Color(0xFFC0C0C0)
        NivelUsuario.ORO -> Color(0xFFFFD700)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colorFondo.copy(alpha = 0.25f))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Nivel ${nivel.etiqueta}",
            style = MaterialTheme.typography.titleMedium,
            color = colorFondo
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PerfilScreenPreview() {
    PikuTheme {
        PerfilScreen()
    }
}
