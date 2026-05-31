package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class RecompensaImagen(
    val id: String,
    @SerializedName("recompensa_id") val recompensaId: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String,
    val orden: Int = 0
)

data class RecompensaImagenesResponse(
    val imagenes: List<RecompensaImagen> = emptyList(),
    @SerializedName("portada_url") val portadaUrl: String? = null,
    @SerializedName("recompensa_id") val recompensaId: String? = null
)

data class RecompensaImagenUploadResponse(
    val mensaje: String? = null,
    val imagen: RecompensaImagen? = null,
    val recompensa: OfertaComercio? = null
)
