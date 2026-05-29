package com.piku.app.data.network

import com.piku.app.data.model.ComercioDetalleResponse
import com.piku.app.data.model.ComerciosResponse
import com.piku.app.data.model.LoginRequest
import com.piku.app.data.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PikuApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/auth/perfil")
    suspend fun perfil(): Map<String, Any>

    @GET("api/public/comercios")
    suspend fun listarComercios(): ComerciosResponse

    @GET("api/public/comercios/{id}")
    suspend fun detalleComercio(@Path("id") id: String): ComercioDetalleResponse

    @GET("api/usuario/saldo")
    suspend fun saldo(): Map<String, Any>

    @GET("api/comercio/recompensas")
    suspend fun recompensasComercio(): Map<String, Any>

    @POST("api/comercio/recompensas")
    suspend fun crearRecompensa(@Body body: Map<String, Any>): Map<String, Any>

    @POST("api/comercio/generar-qr")
    suspend fun generarQr(@Body body: Map<String, Any>): Map<String, Any>

    @GET("api/comercio/estadisticas")
    suspend fun estadisticasComercio(): Map<String, Any>
}
