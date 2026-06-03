package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class RankingComercioItem(
    val posicion: Int,
    val nombre: String,
    val canjes: Int,
    val rubro: String
)

data class RankingComerciosResponse(
    val ranking: List<RankingComercioItem>,
    val mes: String
)

data class DesafioItem(
    val id: String,
    val titulo: String,
    val descripcion: String?,
    val tipo: String,
    val objetivo: Int,
    val recompensa: Int,
    val progreso: Int,
    val completado: Boolean,
    @SerializedName("listoParaCompletar") val listoParaCompletar: Boolean,
    @SerializedName("vigenciaDesde") val vigenciaDesde: String?,
    @SerializedName("vigenciaHasta") val vigenciaHasta: String?
)

data class DesafiosResponse(
    val desafios: List<DesafioItem>
)

data class CompletarDesafioResponse(
    val mensaje: String,
    @SerializedName("puntosOtorgados") val puntosOtorgados: Int,
    val saldo: Int?,
    val desafio: String?
)

data class TopClienteInsight(
    val nombre: String?,
    val email: String?,
    @SerializedName("puntos_ganados") val puntosGanados: Int
)

data class OfertasCanjeadasInsight(
    val total: Int,
    @SerializedName("por_tipo") val porTipo: Map<String, Int>
)

data class ComercioInsightsResponse(
    @SerializedName("puntos_entregados_mes") val puntosEntregadosMes: Int,
    @SerializedName("puntos_entregados_mes_anterior") val puntosEntregadosMesAnterior: Int,
    @SerializedName("variacion_puntos") val variacionPuntos: Double,
    @SerializedName("ofertas_canjeadas") val ofertasCanjeadas: OfertasCanjeadasInsight,
    @SerializedName("top_clientes") val topClientes: List<TopClienteInsight>,
    val recomendacion: String,
    @SerializedName("clientes_recurrentes") val clientesRecurrentes: Int,
    @SerializedName("porcentaje_recurrentes") val porcentajeRecurrentes: Double
)
