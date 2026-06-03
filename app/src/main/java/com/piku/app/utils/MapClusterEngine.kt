package com.piku.app.utils

import com.piku.app.data.model.Comercio
import kotlin.math.cos

sealed class MapMarkerItem {
    data class Single(val comercio: Comercio) : MapMarkerItem()
    data class Cluster(
        val lat: Double,
        val lon: Double,
        val comercios: List<Comercio>
    ) : MapMarkerItem()
}

object MapClusterEngine {

    private const val CLUSTER_MAX_ZOOM = 15.75

    fun debeAgrupar(zoom: Double): Boolean = zoom < CLUSTER_MAX_ZOOM

    fun agrupar(comercios: List<Comercio>, zoom: Double, refLat: Double): List<MapMarkerItem> {
        val validos = comercios.filter { it.lat != null && it.lon != null }
        if (!debeAgrupar(zoom)) {
            return validos.map { MapMarkerItem.Single(it) }
        }

        val cell = cellSizeDegrees(zoom, refLat)
        val grupos = LinkedHashMap<String, MutableList<Comercio>>()
        validos.forEach { c ->
            val lat = c.lat ?: return@forEach
            val lon = c.lon ?: return@forEach
            val key = "${(lat / cell).toLong()}|${(lon / cell).toLong()}"
            grupos.getOrPut(key) { mutableListOf() }.add(c)
        }

        return grupos.values.map { lista ->
            if (lista.size == 1) {
                MapMarkerItem.Single(lista.first())
            } else {
                MapMarkerItem.Cluster(
                    lat = lista.mapNotNull { it.lat }.average(),
                    lon = lista.mapNotNull { it.lon }.average(),
                    comercios = lista
                )
            }
        }
    }

    private fun cellSizeDegrees(zoom: Double, lat: Double): Double {
        val metros = when {
            zoom < 14.5 -> 950.0
            zoom < 15.0 -> 650.0
            zoom < 15.5 -> 420.0
            else -> 280.0
        }
        val cosLat = cos(Math.toRadians(lat)).coerceAtLeast(0.35)
        return metros / (111_000.0 * cosLat)
    }
}
