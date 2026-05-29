package com.piku.app.data.model

/**
 * Movimiento de puntos (ganados o canjeados).
 */
data class Transaccion(
    val id: String,
    val descripcion: String,
    val puntos: Int,
    val fecha: String,
    val tipo: TipoTransaccion
)

enum class TipoTransaccion {
    GANADO,
    CANJEADO
}
