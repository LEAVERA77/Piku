package com.piku.app.data.model

/**
 * Recompensa disponible para canjear con puntos.
 */
data class Recompensa(
    val id: String,
    val nombre: String,
    val puntosRequeridos: Int,
    val icono: String,
    val descripcion: String
)
