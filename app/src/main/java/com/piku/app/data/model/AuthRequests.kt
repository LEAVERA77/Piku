package com.piku.app.data.model

data class RegistroRequest(
    val email: String,
    val password: String,
    val nombre: String,
    val telefono: String? = null,
    val calle: String? = null,
    val numero: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null,
    val codigoPostal: String? = null
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
    val calle: String? = null,
    val numero: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null,
    val codigoPostal: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val tipoComercio: String? = null,
    val categoria: String? = null,
    val codigoInvitacion: String? = null,
    val plan: String? = null
)

data class RegistroComercioGoogleRequest(
    val idToken: String,
    val nombre: String? = null,
    val nombreComercio: String,
    val telefono: String? = null,
    val calle: String? = null,
    val numero: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null,
    val codigoPostal: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val tipoComercio: String? = null,
    val categoria: String? = null,
    val codigoInvitacion: String? = null,
    val plan: String? = null
)
