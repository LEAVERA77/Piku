package com.piku.app.data.nominatim

import android.content.Context
import com.piku.app.data.model.Comercio
import com.piku.app.data.nominatim.OsmComercioMapper.esComercioOsm
import com.piku.app.data.nominatim.OsmComercioMapper.nombreComercio
import com.piku.app.data.nominatim.OsmComercioMapper.toComercioOsm

class NominatimRepository(context: Context) {

    private val api = NominatimClient.get(context)

    suspend fun inferirCalle(lat: Double, lon: Double): String? {
        return try {
            val rev = api.reverseGeocode(lat = lat, lon = lon)
            NominatimAddressFormatter.calleDesde(rev.address, rev.displayName)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun buscarCerca(lat: Double, lon: Double, consulta: String, limite: Int = 6): List<NominatimResult> {
        val q = consulta.trim()
        if (q.length < 2) return emptyList()
        val delta = 0.12
        val viewbox = "${lon - delta},${lat + delta},${lon + delta},${lat - delta}"
        return api.geocode(query = q, viewbox = viewbox, bounded = 1, limit = limite)
    }

    suspend fun resolverDireccion(lat: Double, lon: Double, consulta: String): NominatimResult? {
        val q = consulta.trim()
        if (q.isBlank()) return null
        val calleContexto = inferirCalle(lat, lon)
        val rev = try {
            api.reverseGeocode(lat = lat, lon = lon)
        } catch (_: Exception) {
            null
        }
        val query = NominatimAddressFormatter.consultaGeocode(q, rev?.address)
        val resultados = buscarCerca(lat, lon, query, limite = 5)
        return resultados.firstOrNull()
            ?: calleContexto?.let { ctx ->
                buscarCerca(lat, lon, "$q, $ctx", limite = 3).firstOrNull()
            }
    }

    suspend fun sugerenciasPorUbicacion(lat: Double, lon: Double): List<NominatimResult> {
        val sugerencias = mutableListOf<NominatimResult>()
        try {
            val rev = api.reverseGeocode(lat = lat, lon = lon)
            val calle = NominatimAddressFormatter.calleDesde(rev.address, rev.displayName)
            if (!calle.isNullOrBlank()) {
                sugerencias.add(
                    NominatimResult(
                        lat = rev.lat ?: lat.toString(),
                        lon = rev.lon ?: lon.toString(),
                        displayName = calle,
                        address = rev.address
                    )
                )
            }
        } catch (_: Exception) {
            // sin reverse
        }
        return sugerencias
    }

    /**
     * Comercios/POIs de OpenStreetMap en la zona del usuario que coinciden con [consulta].
     */
    suspend fun buscarComerciosEnZona(
        lat: Double,
        lon: Double,
        consulta: String,
        limite: Int = 15
    ): List<Comercio> {
        val q = consulta.trim()
        if (q.length < 2) return emptyList()
        val resultados = buscarCerca(lat, lon, q, limite = limite)
        return resultados
            .filter { it.esComercioOsm() }
            .filter { coincideConsulta(it, q) }
            .mapNotNull { it.toComercioOsm(lat, lon) }
            .distinctBy { it.id }
    }

    private fun coincideConsulta(result: NominatimResult, consulta: String): Boolean {
        val q = consulta.lowercase()
        val nombre = result.nombreComercio().lowercase()
        if (nombre.contains(q)) return true
        return result.displayName.lowercase().contains(q)
    }
}
