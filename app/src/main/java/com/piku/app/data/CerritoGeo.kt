package com.piku.app.data

import com.piku.app.data.model.Comercio
import com.piku.app.utils.DistanceCalculator

/** Geografía de prueba: Cerrito, Entre Ríos. */
object CerritoGeo {

    const val CENTRO_LAT = -31.9189
    const val CENTRO_LON = -60.6085

    private const val RADIO_ZONA_M = 15_000
    private const val RADIO_CERCA_USUARIO_M = 5_000

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
     * Catálogo fijo en Cerrito con datos vivos de la API (ofertas, ids reales).
     * Garantiza coordenadas correctas en el mapa.
     */
    fun listaMapaCerrito(desdeApi: List<Comercio>): List<Comercio> =
        ComerciosCerritoDemo.lista.map { demo ->
            val api = desdeApi.find { it.nombre.equals(demo.nombre, ignoreCase = true) }
            if (api == null) {
                demo
            } else {
                val (lat, lon) = corregirLatLon(api.lat, api.lon)
                demo.copy(
                    id = api.id,
                    lat = demo.lat,
                    lon = demo.lon,
                    direccion = demo.direccion,
                    cantidadOfertas = api.cantidadOfertas,
                    ofertasNuevas = api.ofertasNuevas,
                    logoUrl = api.logoUrl,
                    realizaEnvios = api.realizaEnvios,
                    tipoComercio = api.tipoComercio ?: demo.tipoComercio,
                    iconoEmoji = api.iconoEmoji ?: demo.iconoEmoji,
                    puntosMinCanje = api.puntosMinCanje
                ).let {
                    // Si la API trae coords válidas en Cerrito, preferirlas (por si se mueven en BD)
                    if (lat != null && lon != null && enZonaCerrito(lat, lon)) {
                        it.copy(lat = lat, lon = lon)
                    } else it
                }
            }
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

    fun filtrarCercaDeUsuario(userLat: Double, userLon: Double, lista: List<Comercio>): List<Comercio> =
        conDistanciaDesde(userLat, userLon, lista).filter {
            (it.distanciaMetros ?: Int.MAX_VALUE) <= RADIO_CERCA_USUARIO_M
        }
}
