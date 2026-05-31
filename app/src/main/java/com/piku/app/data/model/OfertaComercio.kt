package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class OfertaComercio(
    val id: String,
    @SerializedName("comercio_id") val comercioId: String? = null,
    val nombre: String,
    val descripcion: String? = null,
    @SerializedName("puntos_requeridos") val puntosRequeridos: Int,
    val tipo: String? = "producto_gratis",
    @SerializedName("porcentaje_descuento") val porcentajeDescuento: Int? = null,
    @SerializedName("monto_maximo_descuento") val montoMaximoDescuento: Double? = null,
    @SerializedName("producto_nombre") val productoNombre: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null,
    val imagenes: List<RecompensaImagen>? = null,
    @SerializedName("imagenes_urls") val imagenesUrls: List<String>? = null,
    @SerializedName("imagenes_extra") val imagenesExtra: Int? = null,
    @SerializedName("fecha_inicio") val fechaInicio: String? = null,
    @SerializedName("fecha_fin") val fechaFin: String? = null,
    @SerializedName("max_usos_por_usuario") val maxUsosPorUsuario: Int? = 1,
    @SerializedName("max_usos_totales") val maxUsosTotales: Int? = 0,
    @SerializedName("usos_actuales") val usosActuales: Int? = 0,
    val activo: Boolean = true,
    val icono: String? = null
) {
    fun photoUrl(): String = com.piku.app.ui.media.PikuImages.resolve(imagenUrl, id, nombre)

    fun todasLasFotos(): List<String> {
        val urls = imagenesUrls?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        if (urls.isEmpty()) {
            imagenes?.map { it.imagenUrl }?.filter { it.isNotBlank() }?.let { urls.addAll(it) }
        }
        val portada = imagenUrl?.takeIf { it.isNotBlank() }
        if (portada != null && !urls.contains(portada)) urls.add(0, portada)
        if (urls.isEmpty() && portada != null) urls.add(portada)
        if (urls.isEmpty()) urls.add(photoUrl())
        return urls.distinct()
    }

    val cantidadFotos: Int get() = todasLasFotos().size

    val vigente: Boolean
        get() = activo
}

data class RecompensasListResponse(
    val recompensas: List<OfertaComercio> = emptyList()
)

data class RecompensaSingleResponse(
    val recompensa: OfertaComercio
)

data class OfertaStatsResponse(
    val canjes: Int = 0,
    @SerializedName("usuarios_unicos") val usuariosUnicos: Int = 0,
    @SerializedName("usos_actuales") val usosActuales: Int = 0
)

data class ImagenUploadResponse(
    @SerializedName("imagen_url") val imagenUrl: String,
    val recompensa: OfertaComercio? = null
)
