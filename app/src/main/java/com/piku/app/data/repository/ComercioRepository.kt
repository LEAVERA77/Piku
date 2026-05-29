package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.ComercioDetalleResponse
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.utils.DistanceCalculator

class ComercioRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    suspend fun listarComercios(userLat: Double?, userLon: Double?): List<Comercio> {
        val lista = api.listarComercios().comercios
        if (userLat == null || userLon == null) return lista
        return lista.map { c ->
            val dist = if (c.lat != null && c.lon != null) {
                DistanceCalculator.metros(userLat, userLon, c.lat, c.lon)
            } else null
            c.copy(distanciaMetros = dist)
        }.sortedBy { it.distanciaMetros ?: Int.MAX_VALUE }
    }

    suspend fun detalleComercio(id: String): ComercioDetalleResponse =
        api.detalleComercio(id)
}
