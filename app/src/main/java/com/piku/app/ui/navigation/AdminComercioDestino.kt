package com.piku.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

enum class AdminComercioDestino(
    val ruta: String,
    val etiqueta: String,
    val mostrarEnBarra: Boolean = true
) {
    Panel("admin_panel", "Panel"),
    Catalogo("admin_catalogo", "Catálogo"),
    Herramientas("admin_herramientas", "Más");

    companion object {
        val barraInferior = entries.filter { it.mostrarEnBarra }
    }
}

fun AdminComercioDestino.icono(): ImageVector = when (this) {
    AdminComercioDestino.Panel -> Icons.Default.Home
    AdminComercioDestino.Catalogo -> Icons.Default.Storefront
    AdminComercioDestino.Herramientas -> Icons.Default.GridView
}
