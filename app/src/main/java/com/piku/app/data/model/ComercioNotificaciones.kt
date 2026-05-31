package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class NotificacionComercio(
    val id: String,
    @SerializedName("comercio_id") val comercioId: String? = null,
    @SerializedName("usuario_id") val usuarioId: String? = null,
    @SerializedName("recompensa_id") val recompensaId: String? = null,
    @SerializedName("canje_id") val canjeId: String? = null,
    val tipo: String,
    val titulo: String,
    val cuerpo: String,
    val leida: Boolean = false,
    @SerializedName("created_at") val createdAt: String? = null
)

data class NotificacionesResponse(
    val notificaciones: List<NotificacionComercio>,
    val total: Int = 0,
    val limite: Int = 0,
    val offset: Int = 0
)

data class NotificacionesNoLeidasResponse(
    @SerializedName("noLeidas") val noLeidas: Int = 0
)

data class CanjeComercioItem(
    val id: String,
    @SerializedName("usuario_id") val usuarioId: String? = null,
    @SerializedName("recompensa_id") val recompensaId: String? = null,
    @SerializedName("puntos_usados") val puntosUsados: Int,
    @SerializedName("codigo_canje") val codigoCanje: String,
    val estado: String,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("cliente_nombre") val clienteNombre: String? = null,
    @SerializedName("oferta_nombre") val ofertaNombre: String? = null,
    @SerializedName("puntos_requeridos") val puntosRequeridos: Int? = null,
    @SerializedName("oferta_tipo") val ofertaTipo: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null
)

data class HistorialCanjesComercioResponse(
    val canjes: List<CanjeComercioItem>,
    val total: Int = 0,
    val pagina: Int = 1,
    val limite: Int = 20
)
