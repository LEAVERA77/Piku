package com.piku.app.data.model

data class Rubro(
    val id: String,
    val label: String,
    val categorias: List<String> = emptyList()
)

data class RubrosResponse(
    val rubros: List<Rubro>
)

data class ChatPikuRequest(
    val pregunta: String,
    val lat: Double? = null,
    val lon: Double? = null
)

data class ChatPikuResponse(
    val respuesta: String,
    val comercio_sugerido_id: String? = null,
    val via: String? = null
)

data class EventoRequest(
    val tipo_evento: String,
    val comercio_id: String? = null
)
