package com.piku.app.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piku.app.ui.components.BarraNavegacion
import com.piku.app.ui.components.UbicacionPermisoGate
import com.piku.app.ui.screens.CanjesScreen
import com.piku.app.ui.screens.DetalleComercioScreen
import com.piku.app.ui.screens.EscanerScreen
import com.piku.app.ui.screens.MapaScreen
import com.piku.app.ui.screens.PerfilScreen
import com.piku.app.ui.screens.SaldoScreen

@Composable
fun PikuClienteNavGraph(
    navController: NavHostController = rememberNavController(),
    onCerrarSesion: () -> Unit = {}
) {
    UbicacionPermisoGate {
        Scaffold(
            bottomBar = { BarraNavegacion(navController) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = PikuDestino.Saldo.ruta,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn() + slideInHorizontally { it / 4 } },
                exitTransition = { fadeOut() + slideOutHorizontally { -it / 4 } },
                popEnterTransition = { fadeIn() + slideInHorizontally { -it / 4 } },
                popExitTransition = { fadeOut() + slideOutHorizontally { it / 4 } }
            ) {
            composable(PikuDestino.Saldo.ruta) {
                SaldoScreen(
                    onEscanearClick = {
                        navController.navigate(PikuDestino.Escaner.ruta) { launchSingleTop = true }
                    },
                    onCanjearClick = {
                        navController.navigate(PikuDestino.Canjes.ruta) { launchSingleTop = true }
                    }
                )
            }
            composable(PikuDestino.Mapa.ruta) {
                MapaScreen(
                    onVerDetalleComercio = { id ->
                        navController.navigate(PikuDestino.detalleComercio(id))
                    }
                )
            }
            composable(
                route = PikuDestino.DetalleComercio.ruta,
                arguments = listOf(navArgument("comercioId") { type = NavType.StringType })
            ) { entry ->
                DetalleComercioScreen(
                    comercioId = entry.arguments?.getString("comercioId") ?: "",
                    onBack = { navController.popBackStack() }
                )
            }
            composable(PikuDestino.Escaner.ruta) { EscanerScreen() }
            composable(PikuDestino.Canjes.ruta) { CanjesScreen() }
            composable(PikuDestino.Perfil.ruta) {
                PerfilScreen(onCerrarSesion = onCerrarSesion)
            }
            }
        }
    }
}
