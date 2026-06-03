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
    @SerializedName("tipo_comercio") val tipoComercio: String? = null,
    @SerializedName("icono_emoji") val iconoEmoji: String? = null,
    @SerializedName("puntos_min_canje") val puntosMinCanje: Int? = null,
    @SerializedName("cantidad_ofertas") val cantidadOfertas: Int = 0,
    @SerializedName("ofertas_nuevas") val ofertasNuevas: Int = 0,
    @SerializedName("realiza_envios") val realizaEnvios: Boolean = false,
    @SerializedName("envio_gratis") val envioGratis: Boolean = false,
    @SerializedName("costo_envio") val costoEnvio: Double? = null,
    @SerializedName("envio_minimo_compra") val envioMinimoCompra: Double? = null,
    @SerializedName("telefono_contacto") val telefonoContacto: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    /** Distancia en metros desde el usuario (calculada en app). */
    val distanciaMetros: Int? = null
) {
    fun esOpenStreetMap(): Boolean = id.startsWith("osm:")

    fun textoEnvio(): String? {
        if (!realizaEnvios) return null
        if (envioGratis) return "📦 Envíos gratis"
        val min = envioMinimoCompra
        if (min != null && min > 0) {
            val minFmt = if (min % 1.0 == 0.0) min.toInt().toString() else "%.2f".format(min)
            return "📦 Envío gratis desde $$minFmt"
        }
        val costo = costoEnvio
        if (costo != null && costo > 0) {
            val cFmt = if (costo % 1.0 == 0.0) costo.toInt().toString() else "%.2f".format(costo)
            return "📦 Envíos desde $$cFmt"
        }
        return "📦 Hace envíos a domicilio"
    }
}

data class ConfiguracionEnvios(
    @SerializedName("realiza_envios") val realizaEnvios: Boolean = false,
    @SerializedName("envio_gratis") val envioGratis: Boolean = false,
    @SerializedName("costo_envio") val costoEnvio: Double = 0.0,
    @SerializedName("envio_minimo_compra") val envioMinimoCompra: Double? = null,
    @SerializedName("telefono_contacto") val telefonoContacto: String? = null
)

data class ConfiguracionEnviosRequest(
    @SerializedName("realiza_envios") val realizaEnvios: Boolean,
    @SerializedName("envio_gratis") val envioGratis: Boolean = false,
    @SerializedName("costo_envio") val costoEnvio: Double? = null,
    @SerializedName("envio_minimo_compra") val envioMinimoCompra: Double? = null,
    @SerializedName("telefono_contacto") val telefonoContacto: String? = null
)

data class ConfiguracionEnviosResponse(
    val envios: ConfiguracionEnvios,
    val mensaje: String? = null
)

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
    @SerializedName("imagen_url") val imagenUrl: String? = null,
    val tipo: String? = null,
    @SerializedName("porcentaje_descuento") val porcentajeDescuento: Int? = null,
    @SerializedName("producto_nombre") val productoNombre: String? = null,
    val condiciones: String? = null,
    @SerializedName("vigencia_desde") val vigenciaDesde: String? = null,
    @SerializedName("vigencia_hasta") val vigenciaHasta: String? = null,
    val imagenes: List<RecompensaImagen>? = null,
    @SerializedName("imagenes_urls") val imagenesUrls: List<String>? = null,
    @SerializedName("imagenes_extra") val imagenesExtra: Int? = null
) {
    fun photoUrl(cloudName: String? = null): String =
        com.piku.app.ui.media.PikuImages.resolve(imagenUrl, id, nombre, cloudName)

    fun todasLasFotos(cloudName: String? = null): List<String> {
        val urls = imagenesUrls?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
        if (urls.isEmpty()) {
            imagenes?.map { it.imagenUrl }?.filter { it.isNotBlank() }?.let { urls.addAll(it) }
        }
        val portada = imagenUrl?.takeIf { it.isNotBlank() }
            ?: urls.firstOrNull()
        if (portada != null && !urls.contains(portada)) urls.add(0, portada)
        if (urls.isEmpty()) urls.add(photoUrl(cloudName))
        return urls.distinct()
    }

    fun resumenBeneficio(): String = when (tipo) {
        "descuento" -> porcentajeDescuento?.let { "$it% de descuento" } ?: "Descuento"
        "2x1" -> "2x1"
        "producto_gratis" -> productoNombre?.takeIf { it.isNotBlank() } ?: "Producto gratis"
        "envio_gratis" -> "Envío gratis"
        else -> tipo?.replace('_', ' ')?.replaceFirstChar { it.uppercase() }.orEmpty()
    }
}

data class OfertasComercioResponse(
    val ofertas: List<RecompensaPublica> = emptyList()
)

data class RecompensaDetalleResponse(
    val recompensa: RecompensaPublica,
    val comercio: Comercio? = null
)

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
