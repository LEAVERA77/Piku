package com.piku.app.ui.preview

import com.piku.app.data.model.Comercio
import com.piku.app.data.model.Recompensa
import com.piku.app.data.model.Rubro
import com.piku.app.ui.PikuChatSugerencias
import com.piku.app.ui.viewmodel.MensajeChat

/** Datos de ejemplo para @Preview de Compose. */
object PreviewMocks {
    val rubros = listOf(
        Rubro(id = "cafeteria", label = "Cafetería", categorias = listOf("cafeteria", "cafe")),
        Rubro(id = "farmacia", label = "Farmacia", categorias = listOf("farmacia"))
    )

    val comercioPiku = Comercio(
        id = "00000000-0000-0000-0000-000000000001",
        nombre = "Café Central",
        direccion = "Av. Corrientes 1234, CABA",
        lat = -34.6037,
        lon = -58.3816,
        categoria = "cafeteria",
        tipoComercio = "cafeteria",
        iconoEmoji = "☕",
        puntosMinCanje = 100,
        cantidadOfertas = 2,
        distanciaMetros = 420
    )

    val mensajesChat = listOf(
        MensajeChat("piku", "¡Hola! Soy Piku. Preguntame dónde canjear tus puntos."),
        MensajeChat("usuario", "¿Hay cafeterías cerca?")
    )

    val preguntasChat: List<String> = PikuChatSugerencias.preguntas.take(3)

    val recompensa = Recompensa(
        id = "preview-recompensa",
        nombre = "Café gratis",
        puntosRequeridos = 150,
        imageUrl = "",
        descripcion = "Un espresso de cortesía"
    )
}
