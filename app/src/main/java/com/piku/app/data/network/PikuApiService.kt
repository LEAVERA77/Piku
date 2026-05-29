package com.piku.app.data.network

import com.piku.app.data.model.Recompensa
import com.piku.app.data.model.Transaccion
import com.piku.app.data.model.Usuario
import retrofit2.http.GET

/**
 * Contrato de API REST (base URL temporal; respuestas mock en desarrollo).
 */
interface PikuApiService {

    @GET("usuario/perfil")
    suspend fun obtenerUsuario(): Usuario

    @GET("transacciones/recientes")
    suspend fun obtenerTransacciones(): List<Transaccion>

    @GET("recompensas")
    suspend fun obtenerRecompensas(): List<Recompensa>
}
