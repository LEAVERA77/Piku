package com.piku.app.data.nominatim

import com.piku.app.data.model.Comercio
import com.piku.app.utils.DistanceCalculator

private val CLASES_COMERCIO_OSM = setOf(
    "shop", "amenity", "tourism", "craft", "office", "leisure", "building"
)

private val TIPOS_EXCLUIDOS = setOf(
    "house", "residential", "apartments", "street", "road", "suburb",
    "neighbourhood", "city", "town", "village", "hamlet"
)

object OsmComercioMapper {

    fun NominatimResult.esComercioOsm(): Boolean {
        val cls = placeClass?.lowercase()?.trim() ?: return false
        if (cls !in CLASES_COMERCIO_OSM) return false
        val t = type?.lowercase()?.trim()
        if (t != null && t in TIPOS_EXCLUIDOS) return false
        val nombre = nombreComercio()
        return nombre.length >= 2
    }

    fun NominatimResult.nombreComercio(): String {
        val n = name?.trim()
        if (!n.isNullOrBlank()) return n
        return displayName.substringBefore(',').trim().take(80)
    }

    fun NominatimResult.toComercioOsm(userLat: Double, userLon: Double): Comercio? {
        if (!esComercioOsm()) return null
        val latV = lat.toDoubleOrNull() ?: return null
        val lonV = lon.toDoubleOrNull() ?: return null
        val osmKey = when {
            osmType != null && osmId != null -> "osm:${osmType}:${osmId}"
            else -> "osm:${lat}_${lon}_${nombreComercio().hashCode()}"
        }
        val nombre = nombreComercio()
        val distancia = DistanceCalculator.metros(userLat, userLon, latV, lonV)
        return Comercio(
            id = osmKey,
            nombre = nombre,
            direccion = acortarDireccion(displayName),
            lat = latV,
            lon = lonV,
            categoria = mapearCategoria(placeClass, type),
            suscripcionActiva = false,
            distanciaMetros = distancia
        )
    }

    private fun mapearCategoria(placeClass: String?, type: String?): String? {
        val t = type?.lowercase()?.trim()
        if (!t.isNullOrBlank()) return t
        return placeClass?.lowercase()?.trim()
    }
}

private fun acortarDireccion(displayName: String): String {
    val idx = displayName.indexOf(',')
    return if (idx > 0) displayName.substring(0, idx.coerceAtMost(120)).trim()
    else displayName.take(120)
}
