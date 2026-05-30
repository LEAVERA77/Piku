package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class Comercio(
    val id: String,
    @SerializedName("usuario_id") val usuarioId: String? = null,
    val nombre: String,
    val direccion: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    @SerializedName("logo_url") val logoUrl: String? = null,
    @SerializedName("suscripcion_activa") val suscripcionActiva: Boolean = true,
    val categoria: String? = null,
    @SerializedName("puntos_min_canje") val puntosMinCanje: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    /** Distancia en metros desde el usuario (calculada en app). */
    val distanciaMetros: Int? = null
) {
    fun esOpenStreetMap(): Boolean = id.startsWith("osm:")
}

data class ComercioDetalleResponse(
    val comercio: Comercio,
    val recompensas: List<RecompensaPublica> = emptyList()
)

data class RecompensaPublica(
    val id: String,
    val nombre: String,
    val descripcion: String? = null,
    @SerializedName("puntos_requeridos") val puntosRequeridos: Int,
    val icono: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null
) {
    fun photoUrl(): String = com.piku.app.ui.media.PikuImages.resolve(imagenUrl, id, nombre)
}

data class ComerciosResponse(
    val comercios: List<Comercio>
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val mensaje: String? = null,
    val token: String,
    val usuario: UsuarioSesion
)

data class UsuarioSesion(
    val id: String,
    val email: String,
    val nombre: String,
    val rol: String,
    @SerializedName("puntos_saldo") val puntosSaldo: Int? = null,
    @SerializedName("comercio_id") val comercioId: String? = null
)
