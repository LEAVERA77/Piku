package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.model.ActualizarPerfilRequest
import com.piku.app.data.model.PerfilUsuarioDto
import com.piku.app.data.network.ApiErrorParser
import com.piku.app.data.network.RetrofitInstance
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class PerfilRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    suspend fun obtenerPerfil(): PerfilUsuarioDto {
        try {
            val res = api.perfil()
            return res.usuario
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun actualizarPerfil(body: ActualizarPerfilRequest): PerfilUsuarioDto {
        try {
            return api.actualizarPerfil(body).usuario
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun subirAvatar(archivo: File): String {
        try {
            val part = MultipartBody.Part.createFormData(
                "file",
                archivo.name,
                archivo.asRequestBody("image/*".toMediaTypeOrNull())
            )
            return api.subirAvatar(part).avatarUrl
                ?: throw Exception("No se recibió URL del avatar")
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }
}
