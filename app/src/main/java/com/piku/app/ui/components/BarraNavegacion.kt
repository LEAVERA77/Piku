package com.piku.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.piku.app.ui.navigation.PikuDestino
import com.piku.app.ui.navigation.icono
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.VerdePiku

@Composable
fun BarraNavegacion(navController: NavController) {
    val destinos = PikuDestino.entries
    val backStack = navController.currentBackStackEntryAsState()
    val rutaActual = backStack.value?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        destinos.forEach { destino ->
            val seleccionado = rutaActual == destino.ruta
            NavigationBarItem(
                selected = seleccionado,
                onClick = {
                    if (rutaActual != destino.ruta) {
                        navController.navigate(destino.ruta) {
                            popUpTo(PikuDestino.Saldo.ruta) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = destino.icono(),
                        contentDescription = destino.etiqueta
                    )
                },
                label = { Text(destino.etiqueta) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = VerdePiku,
                    selectedTextColor = VerdePiku,
                    indicatorColor = NaranjaPiku.copy(alpha = 0.15f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Preview
@Composable
private fun BarraNavegacionPreview() {
    PikuTheme {
        BarraNavegacion(rememberNavController())
    }
}
