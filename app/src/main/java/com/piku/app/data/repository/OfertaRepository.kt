package com.piku.app.data.repository

import android.content.Context
import android.net.Uri
import com.piku.app.data.model.ImagenUploadResponse
import com.piku.app.data.model.OfertaComercio
import com.piku.app.data.model.OfertaStatsResponse
import com.piku.app.data.model.RecompensaSingleResponse
import com.piku.app.data.model.RecompensasListResponse
import com.piku.app.data.network.RetrofitInstance
import com.piku.app.utils.ImageUploadHelper
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class OfertaRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    suspend fun listar(): List<OfertaComercio> =
        api.recompensasComercioLista().recompensas

    suspend fun obtener(id: String): OfertaComercio =
        api.recompensaComercio(id).recompensa

    suspend fun stats(id: String): OfertaStatsResponse =
        api.recompensaStats(id)

    suspend fun crear(body: Map<String, Any?>): OfertaComercio =
        api.crearRecompensa(body).recompensa
            ?: throw IllegalStateException("Respuesta sin recompensa")

    suspend fun actualizar(id: String, body: Map<String, Any?>): OfertaComercio =
        api.actualizarRecompensa(id, body).recompensa
            ?: throw IllegalStateException("Respuesta sin recompensa")

    suspend fun eliminar(id: String) = api.eliminarRecompensa(id)

    suspend fun duplicar(id: String): OfertaComercio =
        api.duplicarRecompensa(id).recompensa
            ?: throw IllegalStateException("Respuesta sin recompensa")

    suspend fun subirImagen(ofertaId: String, uri: Uri): ImagenUploadResponse {
        val bytes = ImageUploadHelper.comprimirImagen(context, uri)
        val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "oferta.jpg", body)
        return api.subirImagenOferta(ofertaId, part)
    }
}
