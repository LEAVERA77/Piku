package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class CanjeRequest(
    @SerializedName("recompensa_id") val recompensaId: String
)

data class CanjeRecompensaResumen(
    val id: String,
    val nombre: String,
    val comercio: String? = null
)

data class CanjeResponse(
    val mensaje: String,
    @SerializedName("codigo_canje") val codigoCanje: String? = null,
    @SerializedName("puntos_restantes") val puntosRestantes: Int? = null,
    val recompensa: CanjeRecompensaResumen? = null
)

data class SaldoResponse(
    @SerializedName("puntos_saldo") val puntosSaldo: Int = 0,
    val puntos: Int? = null
) {
    fun puntosActuales(): Int = puntosSaldo.takeIf { it > 0 } ?: puntos ?: 0
}
