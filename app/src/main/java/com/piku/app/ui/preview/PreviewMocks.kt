package com.piku.app.ui.preview

import com.piku.app.data.model.CanjeComercioItem
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.ComercioInsightsResponse
import com.piku.app.data.model.DesafioItem
import com.piku.app.data.model.NotificacionComercio
import com.piku.app.data.model.OfertasCanjeadasInsight
import com.piku.app.data.model.PlanSuscripcion
import com.piku.app.data.model.RankingComercioItem
import com.piku.app.data.model.Recompensa
import com.piku.app.data.model.RecompensaDetalleResponse
import com.piku.app.data.model.RecompensaPublica
import com.piku.app.data.model.Rubro
import com.piku.app.data.model.SuscripcionEstadoResponse
import com.piku.app.data.model.TopClienteInsight
import com.piku.app.ui.PikuChatSugerencias
import com.piku.app.ui.media.PikuImages
import com.piku.app.ui.viewmodel.DesafiosUiState
import com.piku.app.ui.viewmodel.HistorialCanjesUiState
import com.piku.app.ui.viewmodel.NotificacionesComercioUiState
import com.piku.app.ui.viewmodel.RankingUiState
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
        realizaEnvios = true,
        envioGratis = true,
        telefonoContacto = "3434567890",
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
        imageUrl = PikuImages.cafe,
        descripcion = "Un espresso de cortesía"
    )

    val recompensaPublica = RecompensaPublica(
        id = "preview-oferta",
        nombre = "Café gratis",
        descripcion = "Un espresso de cortesía en tu próxima visita.",
        puntosRequeridos = 150,
        imagenUrl = PikuImages.cafe,
        tipo = "producto_gratis",
        productoNombre = "Espresso",
        condiciones = "Válido de lunes a viernes. Una vez por cliente.",
        vigenciaDesde = "2026-05-01",
        vigenciaHasta = "2026-06-30"
    )

    val recompensaDetalle = RecompensaDetalleResponse(
        recompensa = recompensaPublica,
        comercio = comercioPiku
    )

    val rankingUiState = RankingUiState(
        cargando = false,
        mes = "mayo 2026",
        ranking = listOf(
            RankingComercioItem(1, "Café Central", 45, "cafeteria"),
            RankingComercioItem(2, "Farmacia del Pueblo", 32, "farmacia"),
            RankingComercioItem(3, "Panadería La Esquina", 28, "panaderia"),
            RankingComercioItem(4, "Kiosco Sol", 15, "kiosco")
        )
    )

    val desafiosUiState = DesafiosUiState(
        cargando = false,
        desafios = listOf(
            DesafioItem(
                id = "1",
                titulo = "Explorador",
                descripcion = "Visitá 3 comercios diferentes esta semana",
                tipo = "visitas",
                objetivo = 3,
                recompensa = 50,
                progreso = 2,
                completado = false,
                listoParaCompletar = false,
                vigenciaDesde = null,
                vigenciaHasta = null
            ),
            DesafioItem(
                id = "2",
                titulo = "Canjeador",
                descripcion = "Canjeá 1 oferta",
                tipo = "canjes",
                objetivo = 1,
                recompensa = 100,
                progreso = 1,
                completado = false,
                listoParaCompletar = true,
                vigenciaDesde = null,
                vigenciaHasta = null
            ),
            DesafioItem(
                id = "3",
                titulo = "Puntos x2",
                descripcion = "Sumá 200 puntos",
                tipo = "puntos",
                objetivo = 200,
                recompensa = 75,
                progreso = 200,
                completado = true,
                listoParaCompletar = false,
                vigenciaDesde = null,
                vigenciaHasta = null
            )
        )
    )

    val insights = ComercioInsightsResponse(
        puntosEntregadosMes = 1250,
        puntosEntregadosMesAnterior = 980,
        variacionPuntos = 27.6,
        ofertasCanjeadas = OfertasCanjeadasInsight(
            total = 18,
            porTipo = mapOf(
                "descuento" to 8,
                "producto_gratis" to 5,
                "2x1" to 3,
                "envio_gratis" to 2
            )
        ),
        topClientes = listOf(
            TopClienteInsight("María G.", "maria@email.com", 320),
            TopClienteInsight("Juan P.", "juan@email.com", 280)
        ),
        recomendacion = "Tus ofertas de descuento tienen buena conversión. Probá publicar una 2x1 los viernes.",
        clientesRecurrentes = 12,
        porcentajeRecurrentes = 40.0
    )

    val historialCanjesUiState = HistorialCanjesUiState(
        cargando = false,
        canjes = listOf(
            CanjeComercioItem(
                id = "1",
                puntosUsados = 150,
                codigoCanje = "PKU-A1B2",
                estado = "completado",
                createdAt = "2026-05-28 14:30",
                clienteNombre = "María García",
                ofertaNombre = "Café gratis"
            ),
            CanjeComercioItem(
                id = "2",
                puntosUsados = 200,
                codigoCanje = "PKU-C3D4",
                estado = "pendiente",
                createdAt = "2026-05-27 10:15",
                clienteNombre = "Juan Pérez",
                ofertaNombre = "20% descuento"
            )
        )
    )

    val notificacionesUiState = NotificacionesComercioUiState(
        cargando = false,
        notificaciones = listOf(
            NotificacionComercio(
                id = "1",
                tipo = "canje",
                titulo = "Nuevo canje",
                cuerpo = "María canjeó Café gratis (150 pts)",
                leida = false,
                createdAt = "Hace 5 min"
            ),
            NotificacionComercio(
                id = "2",
                tipo = "canje",
                titulo = "Canje completado",
                cuerpo = "Juan usó su código PKU-C3D4",
                leida = true,
                createdAt = "Ayer"
            )
        )
    )

    val suscripcionEstado = SuscripcionEstadoResponse(
        plan = "gratuito",
        planNombre = "Gratuito",
        puntosUsadosMes = 320,
        puntosLimite = 500,
        ofertasActivas = 2,
        ofertasLimite = 2,
        planes = listOf(
            PlanSuscripcion("gratuito", "Gratuito", 0.0, 500, 2, false),
            PlanSuscripcion("basico", "Básico", 5.0, 5000, 10, false),
            PlanSuscripcion("pro", "Pro", 15.0, null, null, true)
        )
    )
}
