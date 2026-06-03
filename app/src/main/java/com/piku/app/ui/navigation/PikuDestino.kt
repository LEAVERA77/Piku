package com.piku.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.ui.graphics.vector.ImageVector

enum class PikuDestino(
    val ruta: String,
    val etiqueta: String,
    val mostrarEnBarra: Boolean = true
) {
    Saldo("saldo", "Inicio"),
    Mapa("mapa", "Mapa"),
    Escaner("escaner", "Escanear"),
    Canjes("canjes", "Canjes"),
    Perfil("perfil", "Perfil"),
    DetalleComercio("detalle_comercio/{comercioId}", "Detalle", mostrarEnBarra = false),
    DetalleOferta("detalle_oferta/{recompensaId}", "Oferta", mostrarEnBarra = false),
    Configuracion("configuracion", "Ajustes", mostrarEnBarra = false);

    companion object {
        val barraInferior = entries.filter { it.mostrarEnBarra }
        fun detalleComercio(id: String) = "detalle_comercio/$id"
        fun detalleOferta(id: String) = "detalle_oferta/$id"
    }
}

fun PikuDestino.icono(): ImageVector = when (this) {
    PikuDestino.Saldo -> Icons.Default.Home
    PikuDestino.Mapa -> Icons.Default.Map
    PikuDestino.Escaner -> Icons.Default.QrCodeScanner
    PikuDestino.Canjes -> Icons.Default.CardGiftcard
    PikuDestino.Perfil -> Icons.Default.Person
    PikuDestino.DetalleComercio -> Icons.Default.Map
    PikuDestino.DetalleOferta -> Icons.Default.CardGiftcard
    PikuDestino.Configuracion -> Icons.Default.Person
}

object PikuRutasRoot {
    const val Splash = "splash"
    const val ElegirTipo = "elegir_tipo"
    const val Login = "login"
    const val Cliente = "cliente"
    const val Admin = "admin"
    const val AdminOfertas = "admin_ofertas"
    const val AdminQr = "admin_qr"
    const val AdminConfigEnvios = "admin_config_envios"
    const val AdminGestionOfertas = "admin_gestion_ofertas"
    const val AdminFormOferta = "admin_form_oferta/{ofertaId}"
    const val AdminNotificaciones = "admin_notificaciones"
    const val AdminHistorialCanjes = "admin_historial_canjes"
    const val AdminUbicacion = "admin_ubicacion"
    const val AdminSuscripcion = "admin_suscripcion"
    const val AdminReglasPuntos = "admin_reglas_puntos"

    fun adminFormOferta(ofertaId: String = "new") = "admin_form_oferta/$ofertaId"
}
