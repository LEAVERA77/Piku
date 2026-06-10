package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class EstadisticasComercio(
    @SerializedName("qrUsados") val qrUsados: Int = 0,
    @SerializedName("puntosOtorgados") val puntosOtorgados: Int = 0,
    @SerializedName("canjesRealizados") val canjesRealizados: Int = 0,
    @SerializedName("clientesUnicos") val clientesUnicos: Int = 0
)

data class TransaccionComercio(
    val puntos: Int = 0,
    val tipo: String? = null,
    val descripcion: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    val cliente: String? = null
)

data class EstadisticasComercioResponse(
    val estadisticas: EstadisticasComercio? = null,
    @SerializedName("ultimasTransacciones") val ultimasTransacciones: List<TransaccionComercio> = emptyList()
)
