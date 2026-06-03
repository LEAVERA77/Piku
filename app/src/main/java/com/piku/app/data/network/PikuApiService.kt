package com.piku.app.data.network

import com.piku.app.data.model.ActualizarPerfilRequest
import com.piku.app.data.model.ActualizarPerfilResponse
import com.piku.app.data.model.AvatarUploadResponse
import com.piku.app.data.model.CanjeRequest
import com.piku.app.data.model.CanjeResponse
import com.piku.app.data.model.ChatPikuRequest
import com.piku.app.data.model.PerfilResponse
import com.piku.app.data.model.ChatPikuResponse
import com.piku.app.data.model.ConfiguracionEnviosRequest
import com.piku.app.data.model.HistorialCanjesComercioResponse
import com.piku.app.data.model.NotificacionesNoLeidasResponse
import com.piku.app.data.model.NotificacionesResponse
import com.piku.app.data.model.ConfiguracionEnviosResponse
import com.piku.app.data.model.ComercioDetalleResponse
import com.piku.app.data.model.ComerciosResponse
import com.piku.app.data.model.OfertasComercioResponse
import com.piku.app.data.model.RecompensaDetalleResponse
import com.piku.app.data.model.RecompensaImagenesResponse
import com.piku.app.data.model.RecompensaImagenUploadResponse
import com.piku.app.data.model.EventoRequest
import com.piku.app.data.model.BonificacionResponse
import com.piku.app.data.model.DesglosePuntosResponse
import com.piku.app.data.model.HistorialResponse
import com.piku.app.data.model.RecompensasDisponiblesResponse
import com.piku.app.data.model.RubrosResponse
import com.piku.app.data.model.SaldoApiResponse
import com.piku.app.data.model.SuscripcionEstadoResponse
import com.piku.app.data.model.CambiarPlanRequest
import com.piku.app.data.model.CambiarPlanResponse
import com.piku.app.data.model.GoogleLoginRequest
import com.piku.app.data.model.ImagenUploadResponse
import com.piku.app.data.model.LoginRequest
import com.piku.app.data.model.LogoUploadResponse
import com.piku.app.data.model.LoginResponse
import com.piku.app.data.model.OfertaStatsResponse
import com.piku.app.data.model.RegistroComercioGoogleRequest
import com.piku.app.data.model.RegistroComercioRequest
import com.piku.app.data.model.GenerarQrRequest
import com.piku.app.data.model.GenerarQrResponse
import com.piku.app.data.model.RegistroRequest
import com.piku.app.data.model.ReglasPuntosResponse
import com.piku.app.data.model.ReglasPuntosUpdateResponse
import com.piku.app.data.model.ValidarQrRequest
import com.piku.app.data.model.ValidarQrResponse
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
import retrofit2.http.Query

interface PikuApiService {

    @POST("api/auth/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/auth/registro-cliente")
    suspend fun registroCliente(@Body body: RegistroRequest): LoginResponse

    @POST("api/auth/google")
    suspend fun loginGoogle(@Body body: GoogleLoginRequest): LoginResponse

    @POST("api/auth/registro-comercio")
    suspend fun registroComercio(@Body body: RegistroComercioRequest): LoginResponse

    @POST("api/auth/registro-comercio-google")
    suspend fun registroComercioGoogle(@Body body: RegistroComercioGoogleRequest): LoginResponse

    @GET("api/auth/perfil")
    suspend fun perfil(): PerfilResponse

    @PUT("api/auth/perfil")
    suspend fun actualizarPerfil(@Body body: ActualizarPerfilRequest): ActualizarPerfilResponse

    @Multipart
    @POST("api/usuario/avatar")
    suspend fun subirAvatar(@Part file: MultipartBody.Part): AvatarUploadResponse

    @GET("api/rubros")
    suspend fun listarRubros(): RubrosResponse

    @GET("api/public/comercios")
    suspend fun listarComercios(
        @Query("minLat") minLat: Double? = null,
        @Query("maxLat") maxLat: Double? = null,
        @Query("minLon") minLon: Double? = null,
        @Query("maxLon") maxLon: Double? = null
    ): ComerciosResponse

    @POST("api/chat-piku")
    suspend fun chatPiku(@Body body: ChatPikuRequest): ChatPikuResponse

    @POST("api/usuario/eventos")
    suspend fun registrarEvento(@Body body: EventoRequest): Map<String, Any>

    @GET("api/public/comercios/{id}")
    suspend fun detalleComercio(@Path("id") id: String): ComercioDetalleResponse

    @GET("api/public/comercios/{id}/ofertas")
    suspend fun ofertasComercio(@Path("id") id: String): OfertasComercioResponse

    @GET("api/public/recompensas/{id}")
    suspend fun detalleRecompensa(@Path("id") id: String): RecompensaDetalleResponse

    @GET("api/usuario/saldo")
    suspend fun saldoCliente(): SaldoApiResponse

    @GET("api/usuario/saldo/desglose")
    suspend fun desglosePuntos(): DesglosePuntosResponse

    @GET("api/usuario/historial")
    suspend fun historialPuntos(@Query("limite") limite: Int = 50): HistorialResponse

    @POST("api/usuario/bonificacion/bienvenida")
    suspend fun bonificacionBienvenida(): BonificacionResponse

    @POST("api/usuario/bonificacion/compartir")
    suspend fun bonificacionCompartir(): BonificacionResponse

    @GET("api/usuario/recompensas")
    suspend fun recompensasDisponibles(): RecompensasDisponiblesResponse

    @POST("api/usuario/canjear")
    suspend fun canjearRecompensa(@Body body: CanjeRequest): CanjeResponse

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

    @GET("api/comercio/recompensas/{id}/imagenes")
    suspend fun listarImagenesGaleria(@Path("id") id: String): RecompensaImagenesResponse

    @Multipart
    @POST("api/comercio/recompensas/{id}/imagenes")
    suspend fun subirImagenGaleria(
        @Path("id") id: String,
        @Part file: MultipartBody.Part,
        @retrofit2.http.Query("portada") portada: Int? = null
    ): RecompensaImagenUploadResponse

    @DELETE("api/comercio/recompensas/{id}/imagenes/{imagenId}")
    suspend fun eliminarImagenGaleria(
        @Path("id") id: String,
        @Path("imagenId") imagenId: String
    ): RecompensaSingleResponse

    @PUT("api/comercio/recompensas/{id}/portada")
    suspend fun establecerPortada(
        @Path("id") id: String,
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): RecompensaSingleResponse

    @POST("api/qr/validar")
    suspend fun validarEscaneo(@Body body: ValidarQrRequest): ValidarQrResponse

    @POST("api/comercio/generar-qr")
    suspend fun generarQr(@Body body: GenerarQrRequest): GenerarQrResponse

    @GET("api/comercio/estadisticas")
    suspend fun estadisticasComercio(): Map<String, Any>

    @Multipart
    @POST("api/comercio/upload-logo")
    suspend fun subirLogoComercio(@Part file: MultipartBody.Part): LogoUploadResponse

    @PUT("api/comercio/ubicacion")
    suspend fun actualizarUbicacionComercio(
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): Map<String, Any>

    @GET("api/comercio/envios")
    suspend fun obtenerConfigEnvios(): ConfiguracionEnviosResponse

    @PUT("api/comercio/envios")
    suspend fun actualizarConfigEnvios(@Body body: ConfiguracionEnviosRequest): ConfiguracionEnviosResponse

    @GET("api/comercio/notificaciones")
    suspend fun notificacionesComercio(
        @Query("limite") limite: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("solo_no_leidas") soloNoLeidas: Boolean? = null
    ): NotificacionesResponse

    @GET("api/comercio/notificaciones/no-leidas")
    suspend fun notificacionesNoLeidas(): NotificacionesNoLeidasResponse

    @PUT("api/comercio/notificaciones/{id}/leer")
    suspend fun marcarNotificacionLeida(@Path("id") id: String): Map<String, Any>

    @PUT("api/comercio/dispositivo/fcm")
    suspend fun registrarFcmToken(@Body body: Map<String, String>): Map<String, Any>

    @GET("api/comercio/canjes")
    suspend fun historialCanjesComercio(
        @Query("pagina") pagina: Int = 1,
        @Query("limite") limite: Int = 20,
        @Query("estado") estado: String? = null,
        @Query("fecha_desde") fechaDesde: String? = null,
        @Query("fecha_hasta") fechaHasta: String? = null,
        @Query("buscar") buscar: String? = null
    ): HistorialCanjesComercioResponse

    @GET("api/comercio/suscripcion/estado")
    suspend fun estadoSuscripcionComercio(): SuscripcionEstadoResponse

    @PUT("api/comercio/suscripcion/plan")
    suspend fun cambiarPlanSuscripcion(@Body body: CambiarPlanRequest): CambiarPlanResponse

    @GET("api/comercio/reglas")
    suspend fun obtenerReglasPuntos(): ReglasPuntosResponse

    @PUT("api/comercio/reglas")
    suspend fun actualizarReglasPuntos(@Body body: Map<String, @JvmSuppressWildcards Any?>): ReglasPuntosUpdateResponse
}
