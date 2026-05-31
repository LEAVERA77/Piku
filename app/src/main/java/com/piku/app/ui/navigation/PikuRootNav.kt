package com.piku.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.piku.app.data.repository.AuthRepository
import com.piku.app.ui.screens.ElegirTipoUsuarioScreen
import com.piku.app.ui.screens.LoginScreen
import com.piku.app.ui.screens.SplashScreen
import com.piku.app.ui.navigation.AdminComercioNavGraph
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
                onIrElegirTipo = {
                    navController.navigate(PikuRutasRoot.ElegirTipo) {
                        popUpTo(PikuRutasRoot.Splash) { inclusive = true }
                    }
                },
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
        composable(PikuRutasRoot.ElegirTipo) {
            ElegirTipoUsuarioScreen(
                onContinuar = {
                    navController.navigate(PikuRutasRoot.Login) {
                        popUpTo(PikuRutasRoot.ElegirTipo) { inclusive = true }
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
            AdminComercioNavGraph(
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
    }
}
