package com.piku.app.data.model

data class RegistroRequest(
    val email: String,
    val password: String,
    val nombre: String,
    val telefono: String? = null
)

data class GoogleLoginRequest(
    val idToken: String
)
