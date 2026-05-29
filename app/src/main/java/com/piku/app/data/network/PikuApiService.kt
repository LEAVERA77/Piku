package com.piku.app.data.network

import com.piku.app.data.model.ComercioDetalleResponse
import com.piku.app.data.model.ComerciosResponse
import com.piku.app.data.model.GoogleLoginRequest
import com.piku.app.data.model.ImagenUploadResponse
import com.piku.app.data.model.LoginRequest
import com.piku.app.data.model.LoginResponse
import com.piku.app.data.model.OfertaStatsResponse
import com.piku.app.data.model.RegistroComercioRequest
import com.piku.app.data.model.RegistroRequest
import com.piku.app.data.model.RecompensaSingleResponse
import com.piku.app.data.model.RecompensasListResponse
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface PikuApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/registro-cliente")
    suspend fun registroCliente(@Body body: RegistroRequest): LoginResponse

    @POST("api/auth/google")
    suspend fun loginGoogle(@Body body: GoogleLoginRequest): LoginResponse

    @POST("api/auth/registro-comercio")
    suspend fun registroComercio(@Body body: RegistroComercioRequest): LoginResponse

    @GET("api/auth/perfil")
    suspend fun perfil(): Map<String, Any>

    @GET("api/public/comercios")
    suspend fun listarComercios(): ComerciosResponse

    @GET("api/public/comercios/{id}")
    suspend fun detalleComercio(@Path("id") id: String): ComercioDetalleResponse

    @GET("api/usuario/saldo")
    suspend fun saldo(): Map<String, Any>

    @GET("api/comercio/recompensas")
    suspend fun recompensasComercioLista(): RecompensasListResponse

    @GET("api/comercio/recompensas/{id}")
    suspend fun recompensaComercio(@Path("id") id: String): RecompensaSingleResponse

    @GET("api/comercio/recompensas/{id}/stats")
    suspend fun recompensaStats(@Path("id") id: String): OfertaStatsResponse

    @POST("api/comercio/recompensas")
    suspend fun crearRecompensa(@Body body: Map<String, @JvmSuppressWildcards Any?>): RecompensaSingleResponse

    @PUT("api/comercio/recompensas/{id}")
    suspend fun actualizarRecompensa(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): RecompensaSingleResponse

    @DELETE("api/comercio/recompensas/{id}")
    suspend fun eliminarRecompensa(@Path("id") id: String): Map<String, Any>

    @POST("api/comercio/recompensas/{id}/duplicar")
    suspend fun duplicarRecompensa(@Path("id") id: String): RecompensaSingleResponse

    @Multipart
    @POST("api/comercio/recompensas/{id}/imagen")
    suspend fun subirImagenOferta(
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): ImagenUploadResponse

    @POST("api/comercio/generar-qr")
    suspend fun generarQr(@Body body: Map<String, Any>): Map<String, Any>

    @GET("api/comercio/estadisticas")
    suspend fun estadisticasComercio(): Map<String, Any>
}
