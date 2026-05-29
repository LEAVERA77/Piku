package com.piku.app.data.network

import org.json.JSONObject
import retrofit2.HttpException

object ApiErrorParser {

    fun mensaje(e: HttpException): String {
        val porCodigo = when (e.code()) {
            409 -> "Este email ya está registrado. Probá «Ingresar» o usá otro correo."
            403 -> "No tenés permiso. Revisá el código de invitación."
            401 -> "Credenciales inválidas."
            400 -> "Datos inválidos. Revisá el formulario."
            else -> "Error del servidor (${e.code()})"
        }
        val body = e.response()?.errorBody()?.string() ?: return porCodigo
        return try {
            val json = JSONObject(body)
            json.optString("error").ifBlank { porCodigo }
        } catch (_: Exception) {
            porCodigo
        }
    }
}
