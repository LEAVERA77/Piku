package com.piku.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piku.app.ui.components.BarraNavegacionComercio
import com.piku.app.ui.screens.admin.AdminDashboardScreen
import com.piku.app.ui.screens.admin.AdminGenerarQrScreen
import com.piku.app.ui.screens.admin.AdminHerramientasScreen
import com.piku.app.ui.screens.admin.FormularioOfertaScreen
import com.piku.app.ui.screens.admin.GestionOfertasScreen
import com.piku.app.ui.screens.admin.ComercioUbicacionScreen
import com.piku.app.ui.screens.comercio.HistorialCanjesScreen
import com.piku.app.ui.screens.comercio.ConfiguracionEnvioScreen
import com.piku.app.ui.screens.comercio.NotificacionesComercioScreen
import com.piku.app.ui.screens.comercio.SuscripcionScreen

private val rutasConBarra = setOf(
    AdminComercioDestino.Panel.ruta,
    AdminComercioDestino.Catalogo.ruta,
    AdminComercioDestino.Herramientas.ruta
)

@Composable
fun AdminComercioNavGraph(
    onCerrarSesion: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val backStack = navController.currentBackStackEntryAsState()
    val mostrarBarra = backStack.value?.destination?.route in rutasConBarra

    Scaffold(
        bottomBar = {
            if (mostrarBarra) {
                BarraNavegacionComercio(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AdminComercioDestino.Panel.ruta,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AdminComercioDestino.Panel.ruta) {
                AdminDashboardScreen(
                    onCerrarSesion = onCerrarSesion,
                    onSuscripcion = { navController.navigate(PikuRutasRoot.AdminSuscripcion) }
                )
            }
            composable(AdminComercioDestino.Catalogo.ruta) {
                GestionOfertasScreen(
                    modoTab = true,
                    onBack = {},
                    onNuevaOferta = {
                        navController.navigate(PikuRutasRoot.adminFormOferta("new"))
                    },
                    onEditarOferta = { id ->
                        navController.navigate(PikuRutasRoot.adminFormOferta(id))
                    }
                )
            }
            composable(AdminComercioDestino.Herramientas.ruta) {
                AdminHerramientasScreen(
                    onGenerarQr = { navController.navigate(PikuRutasRoot.AdminQr) },
                    onConfigEnvios = { navController.navigate(PikuRutasRoot.AdminConfigEnvios) },
                    onNotificaciones = { navController.navigate(PikuRutasRoot.AdminNotificaciones) },
                    onHistorialCanjes = { navController.navigate(PikuRutasRoot.AdminHistorialCanjes) },
                    onUbicacion = { navController.navigate(PikuRutasRoot.AdminUbicacion) },
                    onSuscripcion = { navController.navigate(PikuRutasRoot.AdminSuscripcion) }
                )
            }
            composable(
                route = PikuRutasRoot.AdminFormOferta,
                arguments = listOf(navArgument("ofertaId") { type = NavType.StringType })
            ) { entry ->
                FormularioOfertaScreen(
                    ofertaId = entry.arguments?.getString("ofertaId"),
                    onBack = { navController.popBackStack() },
                    onGuardado = { navController.popBackStack() }
                )
            }
            composable(PikuRutasRoot.AdminQr) {
                AdminGenerarQrScreen(onBack = { navController.popBackStack() })
            }
            composable(PikuRutasRoot.AdminConfigEnvios) {
                ConfiguracionEnvioScreen(onBack = { navController.popBackStack() })
            }
            composable(PikuRutasRoot.AdminNotificaciones) {
                NotificacionesComercioScreen(onBack = { navController.popBackStack() })
            }
            composable(PikuRutasRoot.AdminHistorialCanjes) {
                HistorialCanjesScreen(onBack = { navController.popBackStack() })
            }
            composable(PikuRutasRoot.AdminUbicacion) {
                ComercioUbicacionScreen(onBack = { navController.popBackStack() })
            }
            composable(PikuRutasRoot.AdminSuscripcion) {
                SuscripcionScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
