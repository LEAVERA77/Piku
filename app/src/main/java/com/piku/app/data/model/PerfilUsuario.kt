package com.piku.app.data.model

import com.google.gson.annotations.SerializedName

data class PerfilResponse(
    val usuario: PerfilUsuarioDto
)

data class PerfilUsuarioDto(
    val id: String,
    val email: String,
    val nombre: String,
    val telefono: String? = null,
    val rol: String,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("puntos_saldo") val puntosSaldo: Int = 0,
    @SerializedName("direccion_entrega") val direccionEntrega: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null,
    @SerializedName("codigo_postal") val codigoPostal: String? = null,
    @SerializedName("notas_entrega") val notasEntrega: String? = null
)

data class ActualizarPerfilRequest(
    val nombre: String? = null,
    val telefono: String? = null,
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("direccion_entrega") val direccionEntrega: String? = null,
    val ciudad: String? = null,
    val provincia: String? = null,
    @SerializedName("codigo_postal") val codigoPostal: String? = null,
    @SerializedName("notas_entrega") val notasEntrega: String? = null
)

data class ActualizarPerfilResponse(
    val mensaje: String? = null,
    val usuario: PerfilUsuarioDto
)
