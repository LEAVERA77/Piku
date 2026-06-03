package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class PlanSuscripcion(
    val id: String,
    val nombre: String,
    @SerializedName("precioUsd") val precioUsd: Double = 0.0,
    @SerializedName("puntosMes") val puntosMes: Int? = null,
    @SerializedName("ofertasActivas") val ofertasActivas: Int? = null,
    val destacado: Boolean = false
)

data class SuscripcionEstadoResponse(
    val plan: String = "gratuito",
    @SerializedName("planNombre") val planNombre: String? = null,
    @SerializedName("precioUsd") val precioUsd: Double = 0.0,
    @SerializedName("puntos_usados_mes") val puntosUsadosMes: Int = 0,
    @SerializedName("puntos_limite") val puntosLimite: Int? = null,
    @SerializedName("puntos_restantes") val puntosRestantes: Int? = null,
    @SerializedName("porcentaje_uso") val porcentajeUso: Int = 0,
    @SerializedName("ofertas_activas") val ofertasActivas: Int = 0,
    @SerializedName("ofertas_limite") val ofertasLimite: Int? = null,
    val destacado: Boolean = false,
    @SerializedName("limite_alcanzado") val limiteAlcanzado: Boolean = false,
    val planes: List<PlanSuscripcion> = emptyList()
) {
    fun etiquetaPlan(): String = planNombre ?: when (plan) {
        "basico" -> "Básico"
        "pro" -> "Pro"
        else -> "Gratuito"
    }

    fun textoLimitePuntos(): String {
        val limite = puntosLimite
        return if (limite == null) "Ilimitado" else "$limite PP/mes"
    }
}

data class CambiarPlanRequest(
    val plan: String
)

data class CambiarPlanResponse(
    val mensaje: String? = null,
    val plan: String? = null,
    val estado: SuscripcionEstadoResponse? = null
)
