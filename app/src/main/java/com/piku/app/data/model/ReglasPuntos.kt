package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class ReglasPuntos(
    @SerializedName("comercio_id") val comercioId: String? = null,
    @SerializedName("puntos_por_peso") val puntosPorPeso: Double? = null,
    @SerializedName("monto_minimo") val montoMinimo: Double? = null,
    @SerializedName("puntos_fijos") val puntosFijos: Int? = null,
    @SerializedName("max_puntos_por_dia") val maxPuntosPorDia: Int? = null,
    val activo: Boolean? = null
)

data class ReglasPuntosResponse(
    val reglas: ReglasPuntos,
    @SerializedName("sistemaPikuPoints") val sistemaPikuPoints: Map<String, String>? = null
)

data class ReglasPuntosUpdateResponse(
    val mensaje: String? = null,
    val reglas: ReglasPuntos? = null
)
