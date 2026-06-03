package com.piku.app.data.network

import org.json.JSONObject
import retrofit2.HttpException

object ApiErrorParser {

    fun mensaje(e: HttpException): String {
        val porCodigo = when (e.code()) {
            409 -> "Este email ya está registrado. Probá «Ingresar» o usá otro correo."
            404 -> "Recurso no encontrado. Verificá que la API esté actualizada."
            403 -> "No tenés permiso para esta acción. Verificá que iniciaste sesión como comercio."
            401 -> "Credenciales inválidas."
            400 -> "Datos inválidos. Revisá el formulario."
            503 -> "Servicio temporalmente no disponible. Probá más tarde."
            else -> "Error del servidor (${e.code()})"
        }
        val body = e.response()?.errorBody()?.string() ?: return porCodigo
        return try {
            val json = JSONObject(body)
            val detalle = json.optString("detail").ifBlank { json.optString("detalle") }
            val base = json.optString("respuesta").ifBlank {
                json.optString("error").ifBlank { porCodigo }
            }
            if (detalle.isNotBlank() && detalle != base) "$base ($detalle)" else base
        } catch (_: Exception) {
            porCodigo
        }
    }
}
