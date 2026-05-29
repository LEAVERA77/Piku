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

data class RegistroComercioRequest(
    val email: String,
    val password: String,
    val nombre: String,
    val telefono: String? = null,
    val nombreComercio: String,
    val direccion: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val categoria: String? = null,
    val codigoInvitacion: String? = null
)
