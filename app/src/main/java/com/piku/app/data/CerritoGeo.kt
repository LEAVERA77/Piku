package com.piku.app.data

import com.piku.app.data.model.Comercio
import com.piku.app.utils.DistanceCalculator
import com.piku.app.data.TipoComercio

/** Geografía de prueba: Cerrito, Entre Ríos. */
object CerritoGeo {

    /** Plaza Las Colonias, centro urbano de Cerrito (Entre Ríos). */
    const val CENTRO_LAT = -31.5833
    const val CENTRO_LON = -60.0667

    private const val RADIO_ZONA_M = 8_000

    fun enZonaCerrito(lat: Double, lon: Double): Boolean =
        DistanceCalculator.metros(CENTRO_LAT, CENTRO_LON, lat, lon) <= RADIO_ZONA_M

    /** Corrige lat/lon invertidos (error frecuente en BD o APIs). */
    fun corregirLatLon(lat: Double?, lon: Double?): Pair<Double?, Double?> {
        if (lat == null || lon == null) return lat to lon
        val parecenInvertidos = lat in -61.5..-57.5 && lon in -34.5..-30.0
        return if (parecenInvertidos) lon to lat else lat to lon
    }

    fun esComercioDeCerrito(comercio: Comercio): Boolean {
        val (lat, lon) = corregirLatLon(comercio.lat, comercio.lon)
        if (lat != null && lon != null && enZonaCerrito(lat, lon)) return true
        return comercio.direccion?.contains("Cerrito", true) == true ||
            ComerciosCerritoDemo.lista.any { it.nombre.equals(comercio.nombre, true) }
    }

    /**
     * Combina los comercios reales de la API con el catálogo demo de Cerrito.
     * Los datos reales SIEMPRE tienen prioridad: el demo solo completa
     * comercios que la API todavía no devuelve (y corrige lat/lon invertidos).
     */
    fun listaMapaCerrito(desdeApi: List<Comercio>): List<Comercio> {
        val reales = desdeApi.mapNotNull { api ->
            val (lat, lon) = corregirLatLon(api.lat, api.lon)
            val demo = ComerciosCerritoDemo.lista
                .find { it.nombre.equals(api.nombre, ignoreCase = true) }
            val latFinal = lat ?: demo?.lat ?: return@mapNotNull null
            val lonFinal = lon ?: demo?.lon ?: return@mapNotNull null
            api.copy(
                lat = latFinal,
                lon = lonFinal,
                direccion = api.direccion ?: demo?.direccion,
                tipoComercio = api.tipoComercio ?: demo?.tipoComercio,
                categoria = api.categoria
                    ?: demo?.categoria
                    ?: demo?.let { TipoComercio.desdeId(it.tipoComercio).categoria },
                iconoEmoji = api.iconoEmoji ?: demo?.iconoEmoji
            )
        }
        val nombresReales = reales.map { it.nombre.trim().lowercase() }.toSet()
        val demoFaltantes = ComerciosCerritoDemo.lista
            .filter { it.nombre.trim().lowercase() !in nombresReales }
        return reales + demoFaltantes
    }

    fun conDistanciaDesde(
        userLat: Double,
        userLon: Double,
        lista: List<Comercio>
    ): List<Comercio> = lista.mapNotNull { c ->
        val lat = c.lat ?: return@mapNotNull null
        val lon = c.lon ?: return@mapNotNull null
        val dist = DistanceCalculator.metros(userLat, userLon, lat, lon)
        c.copy(distanciaMetros = dist)
    }.sortedBy { it.distanciaMetros }
}
