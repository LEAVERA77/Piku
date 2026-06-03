package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.model.BonificacionResponse
import com.piku.app.data.model.CompletarDesafioResponse
import com.piku.app.data.model.DesafioItem
import com.piku.app.data.model.CanjeRequest
import com.piku.app.data.model.CanjeResponse
import com.piku.app.data.model.DesglosePuntosResponse
import com.piku.app.data.model.Transaccion
import com.piku.app.data.model.ValidarQrRequest
import com.piku.app.data.model.ValidarQrResponse
import com.piku.app.data.model.Recompensa
import com.piku.app.data.model.SaldoApiResponse
import com.piku.app.data.network.ApiErrorParser
import com.piku.app.data.network.RetrofitInstance
import retrofit2.HttpException

class UsuarioRepository(private val context: Context) {

    private val api = RetrofitInstance.api
    private val cloudName = ConfigLoader.cloudinaryCloudName(context)

    suspend fun obtenerSaldo(): SaldoApiResponse {
        try {
            return api.saldoCliente()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun obtenerHistorial(limite: Int = 50): List<Transaccion> {
        try {
            return api.historialPuntos(limite).transacciones.map { it.toTransaccion() }
        } catch (e: HttpException) {
            if (e.code() == 404) return emptyList()
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun asegurarBonoBienvenida(): BonificacionResponse {
        try {
            return api.bonificacionBienvenida()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun bonificacionCompartir(): BonificacionResponse {
        try {
            return api.bonificacionCompartir()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun listarRecompensasDisponibles(): Pair<Int, List<Recompensa>> {
        try {
            val res = api.recompensasDisponibles()
            val lista = res.recompensas.map { it.toRecompensa(cloudName) }
            return res.puntosDisponibles to lista
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

    suspend fun obtenerDesglose(): DesglosePuntosResponse {
        try {
            return api.desglosePuntos()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun validarEscaneo(codigo: String, lat: Double?, lon: Double?): ValidarQrResponse {
        try {
            return api.validarEscaneo(ValidarQrRequest(codigo = codigo, lat = lat, lon = lon))
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun obtenerDesafios(): List<DesafioItem> {
        try {
            return api.desafiosUsuario().desafios
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun completarDesafio(desafioId: String): CompletarDesafioResponse {
        try {
            return api.completarDesafio(desafioId)
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }
}
