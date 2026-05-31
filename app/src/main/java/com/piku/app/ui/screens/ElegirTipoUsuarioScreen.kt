package com.piku.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.data.datastore.AppPreferences
import com.piku.app.ui.components.BotonPiku
import com.piku.app.ui.components.EstiloBotonPiku
import com.piku.app.ui.components.PikuLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ElegirTipoUsuarioScreen(
    onContinuar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun elegir(rol: String) {
        scope.launch {
            withContext(Dispatchers.IO) {
                AppPreferences.guardarTipoUsuarioInicio(context, rol)
            }
            onContinuar()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PikuLogo(showTagline = true)
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "¿Cómo vas a usar Piku?",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Elegí tu perfil. Podés cambiar de cuenta más adelante al iniciar sesión.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Usuario",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Explorá el mapa, sumá puntos en comercios y canjeá recompensas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BotonPiku(
                    texto = "Soy usuario",
                    onClick = { elegir(AppPreferences.ROL_CLIENTE) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Comerciante",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Gestioná tu local, ofertas, QR y puntos para tus clientes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BotonPiku(
                    texto = "Soy comerciante",
                    onClick = { elegir(AppPreferences.ROL_COMERCIO) },
                    modifier = Modifier.fillMaxWidth(),
                    estilo = EstiloBotonPiku.CONTORNO
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewElegirTipoUsuarioScreen() {
    PikuTheme {
        ElegirTipoUsuarioScreen(onContinuar = {})
    }
}
