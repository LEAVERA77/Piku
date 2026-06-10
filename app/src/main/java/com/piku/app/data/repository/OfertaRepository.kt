package com.piku.app.data.repository

import android.content.Context
import android.net.Uri
import com.piku.app.data.model.ImagenUploadResponse
import com.piku.app.data.model.OfertaComercio
import com.piku.app.data.model.OfertaStatsResponse
import com.piku.app.data.network.ApiErrorParser
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.utils.ImageUploadHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException

class OfertaRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    /** Traduce errores HTTP al mensaje de negocio del backend. */
    private suspend fun <T> conMensajeDeError(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun listar(): List<OfertaComercio> = conMensajeDeError {
        api.recompensasComercioLista().recompensas
    }

    suspend fun obtener(id: String): OfertaComercio = conMensajeDeError {
        api.recompensaComercio(id).recompensa
    }

    suspend fun stats(id: String): OfertaStatsResponse = conMensajeDeError {
        api.recompensaStats(id)
    }

    suspend fun crear(body: Map<String, Any?>): OfertaComercio = conMensajeDeError {
        api.crearRecompensa(body).recompensa
            ?: throw IllegalStateException("Respuesta sin recompensa")
    }

    suspend fun actualizar(id: String, body: Map<String, Any?>): OfertaComercio = conMensajeDeError {
        api.actualizarRecompensa(id, body).recompensa
            ?: throw IllegalStateException("Respuesta sin recompensa")
    }

    suspend fun eliminar(id: String) = conMensajeDeError { api.eliminarRecompensa(id) }

    suspend fun duplicar(id: String): OfertaComercio = conMensajeDeError {
        api.duplicarRecompensa(id).recompensa
            ?: throw IllegalStateException("Respuesta sin recompensa")
    }

    suspend fun subirImagen(ofertaId: String, uri: Uri): ImagenUploadResponse = conMensajeDeError {
        val bytes = ImageUploadHelper.comprimirImagen(context, uri)
        val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "oferta.jpg", body)
        api.subirImagenOferta(ofertaId, part)
    }

    suspend fun listarImagenesGaleria(ofertaId: String) = conMensajeDeError {
        api.listarImagenesGaleria(ofertaId)
    }

    suspend fun subirImagenGaleria(ofertaId: String, uri: Uri, comoPortada: Boolean): OfertaComercio =
        conMensajeDeError {
            val bytes = ImageUploadHelper.comprimirImagen(context, uri)
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val part = MultipartBody.Part.createFormData("file", "oferta.jpg", body)
            val res = api.subirImagenGaleria(ofertaId, part, if (comoPortada) 1 else null)
            res.recompensa ?: throw IllegalStateException("Respuesta sin recompensa")
        }

    suspend fun eliminarImagenGaleria(ofertaId: String, imagenId: String): OfertaComercio =
        conMensajeDeError {
            api.eliminarImagenGaleria(ofertaId, imagenId).recompensa
                ?: throw IllegalStateException("Respuesta sin recompensa")
        }

    suspend fun establecerPortada(ofertaId: String, imagenId: String): OfertaComercio =
        conMensajeDeError {
            api.establecerPortada(ofertaId, mapOf("imagenId" to imagenId)).recompensa
                ?: throw IllegalStateException("Respuesta sin recompensa")
        }
}
