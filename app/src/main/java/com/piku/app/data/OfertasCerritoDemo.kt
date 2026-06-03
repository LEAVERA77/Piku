package com.piku.app.data

import com.piku.app.data.model.RecompensaPublica
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Ofertas de demostración para comercios demo del mapa (sin backend). */
object OfertasCerritoDemo {

    private val vigenciaHasta: String
        get() = LocalDate.now().plusDays(30)
            .atStartOfDay()
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) + "Z"

    private val cafe = listOf(
        oferta("demo-of-1", "Café con 2 medialunas", 300, "producto_gratis", "☕"),
        oferta("demo-of-2", "15% OFF en desayunos", 200, "descuento", "☕", 15),
        oferta("demo-of-3", "2x1 en cafés", 250, "2x1", "☕")
    )

    private val pizzeria = listOf(
        oferta("demo-of-4", "Pizza individual gratis", 500, "producto_gratis", "🍕"),
        oferta("demo-of-5", "20% OFF en pizzas grandes", 400, "descuento", "🍕", 20),
        oferta("demo-of-6", "2x1 en porciones", 200, "2x1", "🍕")
    )

    private val farmacia = listOf(
        oferta("demo-of-7", "10% OFF en medicamentos", 300, "descuento", "💊", 10),
        oferta("demo-of-8", "Producto de regalo", 600, "producto_gratis", "💊")
    )

    private val supermercado = listOf(
        oferta("demo-of-9", "10% OFF en compras", 400, "descuento", "🛒", 10),
        oferta("demo-of-10", "Producto sorpresa", 800, "producto_gratis", "🛒"),
        oferta("demo-of-11", "2x1 en productos seleccionados", 300, "2x1", "🛒")
    )

    private val porComercio = mapOf(
        "demo:cafe-martinez" to cafe,
        "demo:pizzeria-don-juan" to pizzeria,
        "demo:farmacia-cerrito" to farmacia,
        "demo:super-el-ahorro" to supermercado
    )

    fun paraComercio(comercioId: String): List<RecompensaPublica> =
        porComercio[comercioId].orEmpty()

    private fun oferta(
        id: String,
        nombre: String,
        puntos: Int,
        tipo: String,
        icono: String,
        porcentaje: Int? = null
    ) = RecompensaPublica(
        id = id,
        nombre = nombre,
        descripcion = "Oferta de prueba en Cerrito",
        puntosRequeridos = puntos,
        icono = icono,
        tipo = tipo,
        porcentajeDescuento = porcentaje,
        vigenciaHasta = vigenciaHasta
    )
}
