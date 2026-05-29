package com.piku.app.data.model

/**
 * Recompensa disponible para canjear con puntos.
 */
data class Recompensa(
    val id: String,
    val nombre: String,
    val puntosRequeridos: Int,
    val imageUrl: String,
    val descripcion: String
)
