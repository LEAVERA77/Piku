package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.model.CanjeRequest
import com.piku.app.data.model.CanjeResponse
import com.piku.app.data.network.ApiErrorParser
import com.piku.app.data.network.RetrofitInstance
import retrofit2.HttpException

class UsuarioRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    suspend fun obtenerSaldo(): Int {
        try {
            val raw = api.saldo()
            val puntos = raw["puntos_saldo"] ?: raw["puntos"] ?: raw["saldo"]
            return when (puntos) {
                is Number -> puntos.toInt()
                is String -> puntos.toIntOrNull() ?: 0
                else -> 0
            }
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun canjearRecompensa(recompensaId: String): CanjeResponse {
        try {
            return api.canjearRecompensa(CanjeRequest(recompensaId = recompensaId))
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }
}
