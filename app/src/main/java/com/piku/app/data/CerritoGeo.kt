package com.piku.app.data

import com.piku.app.data.model.Comercio
import com.piku.app.utils.DistanceCalculator

/** Geografía de prueba: Cerrito, Entre Ríos. */
object CerritoGeo {

    const val CENTRO_LAT = -31.9189
    const val CENTRO_LON = -60.6085

    /** Radio urbano de Cerrito (~12 km). */
    private const val RADIO_ZONA_M = 12_000

    /** Comercios visibles cerca del usuario en el mapa. */
    private const val RADIO_CERCA_USUARIO_M = 4_000

    fun enZonaCerrito(lat: Double, lon: Double): Boolean =
        DistanceCalculator.metros(CENTRO_LAT, CENTRO_LON, lat, lon) <= RADIO_ZONA_M

    fun esComercioDeCerrito(comercio: Comercio): Boolean {
        val lat = comercio.lat ?: return comercio.direccion?.contains("Cerrito", true) == true
        val lon = comercio.lon ?: return comercio.direccion?.contains("Cerrito", true) == true
        return enZonaCerrito(lat, lon) ||
            comercio.direccion?.contains("Cerrito", true) == true
    }

    /** Une API + demo y deja solo comercios de Cerrito. */
    fun mergeComerciosCerrito(desdeApi: List<Comercio>): List<Comercio> {
        val porNombre = linkedMapOf<String, Comercio>()
        ComerciosCerritoDemo.lista.forEach { porNombre[it.nombre.lowercase()] = it }
        desdeApi.filter { esComercioDeCerrito(it) }.forEach { porNombre[it.nombre.lowercase()] = it }
        return porNombre.values.toList()
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

    /** Comercios de Cerrito a ≤4 km del usuario (todos los de prueba entran). */
    fun filtrarCercaDeUsuario(userLat: Double, userLon: Double, lista: List<Comercio>): List<Comercio> =
        conDistanciaDesde(userLat, userLon, lista).filter {
            (it.distanciaMetros ?: Int.MAX_VALUE) <= RADIO_CERCA_USUARIO_M
        }
}
