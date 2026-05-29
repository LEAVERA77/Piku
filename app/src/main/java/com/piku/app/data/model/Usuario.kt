package com.piku.app.data.model

/**
 * Modelo de usuario de la app Piku.
 */
data class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val puntos: Int,
    val nivel: NivelUsuario,
    val avatarUrl: String? = null,
    val codigoQr: String = "PIKU-${id.take(8).uppercase()}"
)

enum class NivelUsuario(val etiqueta: String, val puntosMinimos: Int) {
    BRONCE("Bronce", 0),
    PLATA("Plata", 500),
    ORO("Oro", 1500);

    companion object {
        fun desdePuntos(puntos: Int): NivelUsuario = when {
            puntos >= ORO.puntosMinimos -> ORO
            puntos >= PLATA.puntosMinimos -> PLATA
            else -> BRONCE
        }
    }
}
