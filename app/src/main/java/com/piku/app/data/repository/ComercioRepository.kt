package com.piku.app.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.net.Uri
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.ComercioDetalleResponse
import com.piku.app.data.model.ConfiguracionEnvios
import com.piku.app.data.model.ConfiguracionEnviosRequest
import com.piku.app.data.model.CambiarPlanRequest
import com.piku.app.data.model.CambiarPlanResponse
import com.piku.app.data.model.CanjeComercioItem
import com.piku.app.data.model.SuscripcionEstadoResponse
import com.piku.app.data.model.GenerarQrRequest
import com.piku.app.data.model.GenerarQrResponse
import com.piku.app.data.model.NotificacionComercio
import com.piku.app.data.model.ReglasPuntos
import com.piku.app.data.model.ReglasPuntosResponse
import com.piku.app.data.model.PerfilResponse
import com.piku.app.data.network.ApiErrorParser
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.utils.ImageUploadHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import com.piku.app.utils.DistanceCalculator
import kotlinx.coroutines.tasks.await

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

    suspend fun obtenerPerfil(): PerfilResponse {
        try {
            return api.perfil()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun subirLogo(uri: Uri): String {
        try {
            val bytes = ImageUploadHelper.comprimirImagen(context, uri)
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "logo.jpg", body)
            return api.subirLogoComercio(part).logoUrl
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun actualizarUbicacion(lat: Double, lon: Double, direccion: String?) {
        try {
            val body = buildMap<String, Any> {
                put("lat", lat)
                put("lon", lon)
                if (!direccion.isNullOrBlank()) put("direccion", direccion)
            }
            api.actualizarUbicacionComercio(body)
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacionGps(): Pair<Double, Double>? {
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val location: Location? = try {
            fused.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token
            ).await()
        } catch (_: Exception) {
            null
        } ?: try {
            fused.lastLocation.await()
        } catch (_: Exception) {
            null
        }
        return location?.let { it.latitude to it.longitude }
    }

    suspend fun obtenerConfigEnvios(): ConfiguracionEnvios {
        try {
            return api.obtenerConfigEnvios().envios
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun guardarConfigEnvios(request: ConfiguracionEnviosRequest): ConfiguracionEnvios {
        try {
            return api.actualizarConfigEnvios(request).envios
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun generarQr(monto: Double, lat: Double? = null, lon: Double? = null): GenerarQrResponse {
        try {
            return api.generarQr(GenerarQrRequest(monto = monto, lat = lat, lon = lon))
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun listarNotificaciones(
        limite: Int = 20,
        offset: Int = 0,
        soloNoLeidas: Boolean = false
    ): Pair<List<NotificacionComercio>, Int> {
        try {
            val res = api.notificacionesComercio(
                limite = limite,
                offset = offset,
                soloNoLeidas = if (soloNoLeidas) true else null
            )
            return res.notificaciones to res.total
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun contarNotificacionesNoLeidas(): Int {
        try {
            return api.notificacionesNoLeidas().noLeidas
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun marcarNotificacionLeida(id: String) {
        try {
            api.marcarNotificacionLeida(id)
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun registrarFcmToken(token: String) {
        try {
            api.registrarFcmToken(mapOf("token" to token))
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun historialCanjes(
        pagina: Int = 1,
        limite: Int = 20,
        estado: String? = null,
        buscar: String? = null
    ): Pair<List<CanjeComercioItem>, Int> {
        try {
            val res = api.historialCanjesComercio(
                pagina = pagina,
                limite = limite,
                estado = estado,
                buscar = buscar?.takeIf { it.isNotBlank() }
            )
            return res.canjes to res.total
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun obtenerEstadoSuscripcion(): SuscripcionEstadoResponse {
        try {
            return api.estadoSuscripcionComercio()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun cambiarPlan(plan: String): CambiarPlanResponse {
        try {
            return api.cambiarPlanSuscripcion(CambiarPlanRequest(plan))
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun obtenerReglasPuntos(): ReglasPuntosResponse {
        try {
            return api.obtenerReglasPuntos()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun guardarReglasPuntos(
        montoMinimo: Double,
        puntosFijos: Int,
        maxPuntosPorDia: Int,
        activo: Boolean
    ): ReglasPuntos {
        try {
            val body = mapOf(
                "montoMinimo" to montoMinimo,
                "puntosFijos" to puntosFijos,
                "maxPuntosPorDia" to maxPuntosPorDia,
                "activo" to activo
            )
            return api.actualizarReglasPuntos(body).reglas
                ?: throw Exception("Respuesta inválida del servidor")
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }
}
