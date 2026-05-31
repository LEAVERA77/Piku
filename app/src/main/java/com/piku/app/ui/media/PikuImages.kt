package com.piku.app.ui.media

import com.piku.app.data.model.TipoTransaccion

/**
 * Fotografías modernas (Unsplash) para la UI. Sin emojis ni ilustraciones.
 */
object PikuImages {

    private fun u(photoId: String, width: Int = 800) =
        "https://images.unsplash.com/$photoId?auto=format&fit=crop&w=$width&q=85"

    val heroApp = u("photo-1556742049-0cfed4f6a45d", 1200)
    val heroLogin = u("photo-1557821552-171051affad3", 1200)
    val heroSaldo = u("photo-1579621970563-ebec756aff7c", 1000)
    val avatarDefault = u("photo-1494790108377-be9c29b29330", 400)
    val escanearQr = u("photo-1606814892909-2a41365ff3f2", 600)
    val canjearPuntos = u("photo-1542838132-92c53300491e", 600)
    val emptyRecompensas = u("photo-1513885535751-8b9238bd345a", 800)
    val permisoCamara = u("photo-1516035069371-29a1a24432a4", 600)
    val comercioDefault = u("photo-1441986300917-64674bd600d8", 600)
    val mapaHeader = u("photo-1526778548025-fa2cfcdcd524", 800)

    val cafe = u("photo-1495474472287-4d71bcdd2085", 600)
    val panaderia = u("photo-1555507036-ab1f4038808a", 600)
    val restaurante = u("photo-1414235077428-338989a2e8c0", 600)
    val envio = u("photo-1566576912321-d58ddd7a6088", 600)
    val descuento = u("photo-1607082349566-187342175e2f", 600)
    val regalo = u("photo-1549465220-1a8f923d4365", 600)

    fun forRecompensaId(id: String): String = when (id) {
        "r1" -> cafe
        "r2" -> panaderia
        "r3" -> restaurante
        "r4" -> envio
        else -> forRecompensaNombre(id)
    }

    fun forRecompensaNombre(nombre: String): String {
        val n = nombre.lowercase()
        return when {
            n.contains("café") || n.contains("cafe") || n.contains("coffee") -> cafe
            n.contains("pan") || n.contains("bakery") || n.contains("panader") -> panaderia
            n.contains("restaurant") || n.contains("comida") || n.contains("menú") -> restaurante
            n.contains("envío") || n.contains("envio") || n.contains("delivery") -> envio
            n.contains("%") || n.contains("descuento") -> descuento
            else -> regalo
        }
    }

    fun cloudinaryOptimized(url: String, cloudName: String, width: Int = 800, height: Int = 600): String {
        if (!url.contains("res.cloudinary.com")) return url
        if (url.contains("/upload/") && url.contains("q_auto")) return url
        return url.replace(
            "/upload/",
            "/upload/w_$width,h_$height,c_limit,q_auto,f_auto/"
        )
    }

    fun resolve(url: String?, id: String? = null, nombre: String? = null, cloudName: String? = null): String {
        if (!url.isNullOrBlank() && url.startsWith("http")) {
            return if (cloudName != null && url.contains("res.cloudinary.com/$cloudName")) {
                cloudinaryOptimized(url, cloudName)
            } else url
        }
        if (!id.isNullOrBlank()) return forRecompensaId(id)
        if (!nombre.isNullOrBlank()) return forRecompensaNombre(nombre)
        return regalo
    }

    fun forTransaccion(descripcion: String, tipo: TipoTransaccion): String {
        val d = descripcion.lowercase()
        return when {
            d.contains("café") || d.contains("cafe") -> cafe
            d.contains("panader") -> panaderia
            d.contains("farmacia") -> u("photo-1584308664894-044658f1ff0f", 400)
            d.contains("qr") || d.contains("compra") -> if (tipo == TipoTransaccion.GANADO) heroSaldo else descuento
            d.contains("bono") -> regalo
            tipo == TipoTransaccion.CANJEADO -> canjearPuntos
            else -> heroSaldo
        }
    }
}
