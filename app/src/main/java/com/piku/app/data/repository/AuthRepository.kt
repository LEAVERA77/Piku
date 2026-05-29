package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.data.model.GoogleLoginRequest
import com.piku.app.data.model.LoginRequest
import com.piku.app.data.model.LoginResponse
import com.piku.app.data.model.RegistroComercioRequest
import com.piku.app.data.model.RegistroRequest
import com.piku.app.data.network.RetrofitInstance

class AuthRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    suspend fun login(email: String, password: String): LoginResponse {
        val response = api.login(LoginRequest(email.trim().lowercase(), password))
        guardarSesion(response)
        return response
    }

    suspend fun registro(
        nombre: String,
        email: String,
        password: String,
        telefono: String? = null
    ): LoginResponse {
        val response = api.registroCliente(
            RegistroRequest(
                email = email.trim().lowercase(),
                password = password,
                nombre = nombre.trim(),
                telefono = telefono?.trim()?.ifEmpty { null }
            )
        )
        guardarSesion(response)
        return response
    }

    suspend fun registroComercio(
        nombre: String,
        email: String,
        password: String,
        nombreComercio: String,
        telefono: String? = null,
        direccion: String? = null,
        categoria: String? = null,
        codigoInvitacion: String? = null
    ): LoginResponse {
        val response = api.registroComercio(
            RegistroComercioRequest(
                email = email.trim().lowercase(),
                password = password,
                nombre = nombre.trim(),
                telefono = telefono?.trim()?.ifEmpty { null },
                nombreComercio = nombreComercio.trim(),
                direccion = direccion?.trim()?.ifEmpty { null },
                categoria = categoria?.trim()?.ifEmpty { null },
                codigoInvitacion = codigoInvitacion?.trim()?.ifEmpty { null }
            )
        )
        guardarSesion(response)
        return response
    }

    suspend fun loginGoogle(idToken: String): LoginResponse {
        val response = api.loginGoogle(GoogleLoginRequest(idToken))
        guardarSesion(response)
        return response
    }

    private suspend fun guardarSesion(response: LoginResponse) {
        AuthDataStore.saveSession(
            context = context,
            token = response.token,
            email = response.usuario.email,
            rol = response.usuario.rol,
            nombre = response.usuario.nombre
        )
    }

    suspend fun logout() = AuthDataStore.clear(context)

    suspend fun hasSession(): Boolean = AuthDataStore.hasSession(context)

    suspend fun isComercio(): Boolean = AuthDataStore.rol(context) == "comercio"
}
