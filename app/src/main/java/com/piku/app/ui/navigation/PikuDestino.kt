package com.piku.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

enum class PikuDestino(
    val ruta: String,
    val etiqueta: String
) {
    Saldo("saldo", "Inicio"),
    Escaner("escaner", "Escanear"),
    Canjes("canjes", "Canjes"),
    Perfil("perfil", "Perfil")
}

fun PikuDestino.icono(): ImageVector = when (this) {
    PikuDestino.Saldo -> Icons.Default.Home
    PikuDestino.Escaner -> Icons.Default.QrCodeScanner
    PikuDestino.Canjes -> Icons.Default.CardGiftcard
    PikuDestino.Perfil -> Icons.Default.Person
}
