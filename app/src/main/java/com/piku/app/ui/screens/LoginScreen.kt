package com.piku.app.ui.screens

import androidx.compose.runtime.Composable

/** @see AuthScreen */
@Composable
fun LoginScreen(
    onLoginCliente: () -> Unit,
    onLoginComercio: () -> Unit
) = AuthScreen(onLoginCliente, onLoginComercio)
