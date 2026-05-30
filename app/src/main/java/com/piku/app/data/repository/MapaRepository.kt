package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.model.ChatPikuRequest
import com.piku.app.data.model.ChatPikuResponse
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.EventoRequest
import com.piku.app.data.model.Rubro
import com.piku.app.data.network.ApiErrorParser
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.utils.DistanceCalculator
import retrofit2.HttpException

class MapaRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    suspend fun listarRubros(): List<Rubro> = api.listarRubros().rubros

    suspend fun listarComerciosInicial(userLat: Double?, userLon: Double?): List<Comercio> {
        try {
            return enriquecerDistancia(api.listarComercios().comercios, userLat, userLon)
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun listarComerciosEnViewport(
        userLat: Double?,
        userLon: Double?,
        minLat: Double,
        maxLat: Double,
        minLon: Double,
        maxLon: Double
    ): List<Comercio> {
        try {
            val lista = api.listarComercios(minLat, maxLat, minLon, maxLon).comercios
            return enriquecerDistancia(lista, userLat, userLon)
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    private fun enriquecerDistancia(
        lista: List<Comercio>,
        userLat: Double?,
        userLon: Double?
    ): List<Comercio> {
        if (userLat == null || userLon == null) return lista
        return lista.map { c ->
            val dist = if (c.lat != null && c.lon != null) {
                DistanceCalculator.metros(userLat, userLon, c.lat, c.lon)
            } else null
            c.copy(distanciaMetros = dist)
        }.sortedBy { it.distanciaMetros ?: Int.MAX_VALUE }
    }

    suspend fun chatPiku(pregunta: String, lat: Double?, lon: Double?): ChatPikuResponse {
        try {
            return api.chatPiku(ChatPikuRequest(pregunta = pregunta, lat = lat, lon = lon))
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun registrarEvento(tipo: String, comercioId: String? = null) {
        try {
            api.registrarEvento(EventoRequest(tipo_evento = tipo, comercio_id = comercioId))
        } catch (_: Exception) {
            // registro silencioso
        }
    }
}
