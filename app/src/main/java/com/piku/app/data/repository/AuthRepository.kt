package com.piku.app.data.repository

import android.content.Context
import com.piku.app.data.datastore.AuthDataStore
import com.piku.app.data.model.LoginRequest
import com.piku.app.data.model.LoginResponse
import com.piku.app.data.network.RetrofitInstance

class AuthRepository(private val context: Context) {

    private val api = RetrofitInstance.api

    suspend fun login(email: String, password: String): LoginResponse {
        val response = api.login(LoginRequest(email.trim().lowercase(), password))
        AuthDataStore.saveSession(
            context = context,
            token = response.token,
            email = response.usuario.email,
            rol = response.usuario.rol,
            nombre = response.usuario.nombre
        )
        return response
    }

    suspend fun logout() = AuthDataStore.clear(context)

    suspend fun hasSession(): Boolean = AuthDataStore.hasSession(context)

    suspend fun isComercio(): Boolean = AuthDataStore.rol(context) == "comercio"
}
