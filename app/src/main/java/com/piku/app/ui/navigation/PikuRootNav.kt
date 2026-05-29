package com.piku.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.piku.app.data.repository.AuthRepository
import com.piku.app.ui.screens.LoginScreen
import com.piku.app.ui.screens.SplashScreen
import com.piku.app.ui.screens.admin.AdminDashboardScreen
import com.piku.app.ui.screens.admin.AdminGenerarQrScreen
import com.piku.app.ui.screens.admin.FormularioOfertaScreen
import com.piku.app.ui.screens.admin.GestionOfertasScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun PikuRootNav() {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = PikuRutasRoot.Splash) {
        composable(PikuRutasRoot.Splash) {
            SplashScreen(
                onIrLogin = {
                    navController.navigate(PikuRutasRoot.Login) {
                        popUpTo(PikuRutasRoot.Splash) { inclusive = true }
                    }
                },
                onIrCliente = {
                    navController.navigate(PikuRutasRoot.Cliente) {
                        popUpTo(PikuRutasRoot.Splash) { inclusive = true }
                    }
                },
                onIrComercio = {
                    navController.navigate(PikuRutasRoot.Admin) {
                        popUpTo(PikuRutasRoot.Splash) { inclusive = true }
                    }
                }
            )
        }
        composable(PikuRutasRoot.Login) {
            LoginScreen(
                onLoginCliente = {
                    navController.navigate(PikuRutasRoot.Cliente) {
                        popUpTo(PikuRutasRoot.Login) { inclusive = true }
                    }
                },
                onLoginComercio = {
                    navController.navigate(PikuRutasRoot.Admin) {
                        popUpTo(PikuRutasRoot.Login) { inclusive = true }
                    }
                }
            )
        }
        composable(PikuRutasRoot.Cliente) {
            PikuClienteNavGraph(
                onCerrarSesion = {
                    CoroutineScope(Dispatchers.Main).launch {
                        AuthRepository(context).logout()
                        navController.navigate(PikuRutasRoot.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(PikuRutasRoot.Admin) {
            AdminDashboardScreen(
                onOfertas = { navController.navigate(PikuRutasRoot.AdminGestionOfertas) },
                onGenerarQr = { navController.navigate(PikuRutasRoot.AdminQr) },
                onCerrarSesion = {
                    CoroutineScope(Dispatchers.Main).launch {
                        AuthRepository(context).logout()
                        navController.navigate(PikuRutasRoot.Login) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
            )
        }
        composable(PikuRutasRoot.AdminGestionOfertas) {
            GestionOfertasScreen(
                onBack = { navController.popBackStack() },
                onNuevaOferta = {
                    navController.navigate(PikuRutasRoot.adminFormOferta("new"))
                },
                onEditarOferta = { id ->
                    navController.navigate(PikuRutasRoot.adminFormOferta(id))
                }
            )
        }
        composable(
            route = PikuRutasRoot.AdminFormOferta,
            arguments = listOf(navArgument("ofertaId") { type = NavType.StringType })
        ) { entry ->
            val ofertaId = entry.arguments?.getString("ofertaId")
            FormularioOfertaScreen(
                ofertaId = ofertaId,
                onBack = { navController.popBackStack() },
                onGuardado = {
                    navController.popBackStack()
                }
            )
        }
        composable(PikuRutasRoot.AdminQr) {
            AdminGenerarQrScreen(onBack = { navController.popBackStack() })
        }
    }
}
