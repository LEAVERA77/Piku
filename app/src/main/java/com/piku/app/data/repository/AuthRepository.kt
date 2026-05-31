package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.data.model.GoogleLoginRequest
import com.piku.app.data.model.LoginRequest
import com.piku.app.data.model.LoginResponse
import com.piku.app.data.model.RegistroComercioGoogleRequest
import com.piku.app.data.model.RegistroComercioRequest
import com.piku.app.data.model.RegistroRequest
import com.piku.app.data.network.ApiErrorParser
import com.piku.app.data.network.RetrofitInstance
import retrofit2.HttpException

class AuthRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    private suspend fun <T> apiCall(block: suspend () -> T): T {
        try {
            return block()
        } catch (e: HttpException) {
            throw Exception(ApiErrorParser.mensaje(e), e)
        }
    }

    suspend fun login(email: String, password: String): LoginResponse {
        val response = apiCall { api.login(LoginRequest(email.trim().lowercase(), password)) }
        guardarSesion(response)
        return response
    }

    suspend fun registro(
        nombre: String,
        email: String,
        password: String,
        telefono: String? = null,
        calle: String,
        numero: String,
        ciudad: String,
        provincia: String,
        codigoPostal: String? = null
    ): LoginResponse {
        val response = apiCall {
            api.registroCliente(
                RegistroRequest(
                    email = email.trim().lowercase(),
                    password = password,
                    nombre = nombre.trim(),
                    telefono = telefono?.trim()?.ifEmpty { null },
                    calle = calle.trim(),
                    numero = numero.trim(),
                    ciudad = ciudad.trim(),
                    provincia = provincia.trim(),
                    codigoPostal = codigoPostal?.trim()?.ifEmpty { null }
                )
            )
        }
        guardarSesion(response)
        return response
    }

    suspend fun registroComercio(
        nombre: String,
        email: String,
        password: String,
        nombreComercio: String,
        telefono: String? = null,
        calle: String,
        numero: String,
        ciudad: String,
        provincia: String,
        codigoPostal: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        tipoComercio: String,
        codigoInvitacion: String? = null
    ): LoginResponse {
        val tipo = com.piku.app.data.TipoComercio.desdeId(tipoComercio)
        val response = apiCall {
            api.registroComercio(
                RegistroComercioRequest(
                    email = email.trim().lowercase(),
                    password = password,
                    nombre = nombre.trim(),
                    telefono = telefono?.trim()?.ifEmpty { null },
                    nombreComercio = nombreComercio.trim(),
                    calle = calle.trim(),
                    numero = numero.trim(),
                    ciudad = ciudad.trim(),
                    provincia = provincia.trim(),
                    codigoPostal = codigoPostal?.trim()?.ifEmpty { null },
                    lat = lat,
                    lon = lon,
                    tipoComercio = tipo.id,
                    categoria = tipo.categoria,
                    codigoInvitacion = codigoInvitacion?.trim()?.ifEmpty { null }
                )
            )
        }
        guardarSesion(response)
        return response
    }

    suspend fun registroComercioGoogle(
        idToken: String,
        nombre: String?,
        nombreComercio: String,
        telefono: String? = null,
        calle: String,
        numero: String,
        ciudad: String,
        provincia: String,
        codigoPostal: String? = null,
        lat: Double? = null,
        lon: Double? = null,
        tipoComercio: String,
        codigoInvitacion: String? = null
    ): LoginResponse {
        val tipo = com.piku.app.data.TipoComercio.desdeId(tipoComercio)
        val response = apiCall {
            api.registroComercioGoogle(
                RegistroComercioGoogleRequest(
                    idToken = idToken,
                    nombre = nombre?.trim()?.ifEmpty { null },
                    nombreComercio = nombreComercio.trim(),
                    telefono = telefono?.trim()?.ifEmpty { null },
                    calle = calle.trim(),
                    numero = numero.trim(),
                    ciudad = ciudad.trim(),
                    provincia = provincia.trim(),
                    codigoPostal = codigoPostal?.trim()?.ifEmpty { null },
                    lat = lat,
                    lon = lon,
                    tipoComercio = tipo.id,
                    categoria = tipo.categoria,
                    codigoInvitacion = codigoInvitacion?.trim()?.ifEmpty { null }
                )
            )
        }
        guardarSesion(response)
        return response
    }

    suspend fun loginGoogle(idToken: String): LoginResponse {
        val response = apiCall { api.loginGoogle(GoogleLoginRequest(idToken)) }
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

    /**
     * Comprueba que el token guardado siga siendo válido en el servidor.
     * Si expiró o la app se reinstaló con datos de backup obsoletos, limpia la sesión.
     */
    suspend fun validarSesionRemota(): Boolean {
        if (!hasSession()) return false
        return try {
            api.perfil()
            true
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                AuthDataStore.clear(context)
                false
            } else {
                true
            }
        } catch (_: Exception) {
            true
        }
    }

    suspend fun isComercio(): Boolean = AuthDataStore.rol(context) == "comercio"
}
