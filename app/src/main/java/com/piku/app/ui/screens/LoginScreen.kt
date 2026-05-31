package com.piku.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.piku.app.ui.theme.PikuTheme

/** @see AuthScreen */
@Composable
fun LoginScreen(
    onLoginCliente: () -> Unit,
    onLoginComercio: () -> Unit
) = AuthScreen(onLoginCliente, onLoginComercio)

@Preview(showBackground = true)
@Composable
fun PreviewLoginScreen() {
    PikuTheme {
        LoginScreen(onLoginCliente = {}, onLoginComercio = {})
    }
}
