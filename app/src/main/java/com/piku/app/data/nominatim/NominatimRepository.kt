package com.piku.app.data.nominatim

import android.content.Context

class NominatimRepository(context: Context) {

    private val api = NominatimClient.get(context)

    suspend fun buscarCerca(lat: Double, lon: Double, consulta: String, limite: Int = 6): List<NominatimResult> {
        val q = consulta.trim()
        if (q.length < 2) return emptyList()
        val delta = 0.12
        val viewbox = "${lon - delta},${lat + delta},${lon + delta},${lat - delta}"
        return api.geocode(query = q, viewbox = viewbox, bounded = 1, limit = limite)
    }

    suspend fun sugerenciasPorUbicacion(lat: Double, lon: Double): List<NominatimResult> {
        val sugerencias = mutableListOf<NominatimResult>()
        try {
            val rev = api.reverseGeocode(lat = lat, lon = lon)
            rev.displayName?.let { nombre ->
                sugerencias.add(
                    NominatimResult(lat = lat.toString(), lon = lon.toString(), displayName = nombre)
                )
            }
        } catch (_: Exception) {
            // sin reverse
        }
        val extras = listOf("cerca de mí", "cafeterías", "farmacia", "supermercado")
        for (prefijo in extras) {
            try {
                val delta = 0.08
                val viewbox = "${lon - delta},${lat + delta},${lon + delta},${lat - delta}"
                val res = api.geocode(query = prefijo, viewbox = viewbox, bounded = 1, limit = 2)
                res.forEach { r ->
                    if (sugerencias.none { it.displayName == r.displayName }) {
                        sugerencias.add(r)
                    }
                }
            } catch (_: Exception) {
                // omitir
            }
        }
        return sugerencias.take(8)
    }
}
