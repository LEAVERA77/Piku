package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class GenerarQrRequest(
    val monto: Double,
    val lat: Double? = null,
    val lon: Double? = null
)

data class QrGenerado(
    val id: String? = null,
    val codigo: String,
    @SerializedName("monto_transaccion") val montoTransaccion: Double? = null,
    @SerializedName("puntos_calculados") val puntosCalculados: Int? = null,
    @SerializedName("expira_at") val expiraAt: String? = null
)

data class GenerarQrResponse(
    val mensaje: String? = null,
    val qr: QrGenerado,
    @SerializedName("expiraEnMinutos") val expiraEnMinutos: Int? = null
)

data class ValidarQrRequest(
    val codigo: String,
    val lat: Double? = null,
    val lon: Double? = null
)

data class ValidarQrResponse(
    val mensaje: String,
    @SerializedName("puntosGanados") val puntosGanados: Int,
    @SerializedName("saldoActual") val saldoActual: Int,
    val comercio: String? = null,
    @SerializedName("distanciaMetros") val distanciaMetros: Int? = null,
    @SerializedName("valorCanjeArs") val valorCanjeArs: Int? = null,
    @SerializedName("pesosPorDolar") val pesosPorDolar: Double? = null,
    @SerializedName("valorPuntoUsd") val valorPuntoUsd: Double? = null
)
