package com.piku.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.piku.app.ui.navigation.AdminComercioDestino
import com.piku.app.ui.navigation.icono
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku

@Composable
fun BarraNavegacionComercio(navController: NavController) {
    val destinos = AdminComercioDestino.barraInferior
    val rutaActual = navController.currentBackStackEntryAsState().value?.destination?.route

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
                            popUpTo(AdminComercioDestino.Panel.ruta) { saveState = true }
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
                    indicatorColor = NaranjaPiku.copy(alpha = 0.18f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
