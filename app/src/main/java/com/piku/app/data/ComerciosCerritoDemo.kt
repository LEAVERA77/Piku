package com.piku.app.data

import com.piku.app.data.model.Comercio

/**
 * Comercios de prueba en Cerrito (Entre Ríos) cuando la API no devuelve datos.
 * Coordenadas en el centro urbano (Plaza Las Colonias), alineadas con [seedComerciosCerrito.js].
 */
object ComerciosCerritoDemo {

    val lista: List<Comercio> = listOf(
        Comercio(
            id = "demo:cafe-martinez",
            nombre = "Café Martínez",
            direccion = "Belgrano 345, Cerrito, Entre Ríos",
            lat = -31.5830,
            lon = -60.0660,
            tipoComercio = "cafeteria",
            categoria = TipoComercio.CAFETERIA.categoria,
            iconoEmoji = "☕",
            realizaEnvios = true,
            cantidadOfertas = 3
        ),
        Comercio(
            id = "demo:pizzeria-don-juan",
            nombre = "Pizzería Don Juan",
            direccion = "San Martín 890, Cerrito, Entre Ríos",
            lat = -31.5840,
            lon = -60.0655,
            tipoComercio = "restaurante",
            categoria = TipoComercio.RESTAURANTE.categoria,
            iconoEmoji = "🍽️",
            realizaEnvios = true,
            cantidadOfertas = 3
        ),
        Comercio(
            id = "demo:farmacia-cerrito",
            nombre = "Farmacia Cerrito",
            direccion = "25 de Mayo 123, Cerrito, Entre Ríos",
            lat = -31.5825,
            lon = -60.0675,
            tipoComercio = "farmacia",
            categoria = TipoComercio.FARMACIA.categoria,
            iconoEmoji = "💊",
            realizaEnvios = true,
            cantidadOfertas = 2
        ),
        Comercio(
            id = "demo:moda-urbana",
            nombre = "Moda Urbana",
            direccion = "Rivadavia 567, Cerrito, Entre Ríos",
            lat = -31.5820,
            lon = -60.0658,
            tipoComercio = "ropa",
            categoria = TipoComercio.ROPA.categoria,
            iconoEmoji = "👕",
            realizaEnvios = true,
            cantidadOfertas = 2
        ),
        Comercio(
            id = "demo:super-el-ahorro",
            nombre = "Supermercado El Ahorro",
            direccion = "Urquiza 789, Cerrito, Entre Ríos",
            lat = -31.5845,
            lon = -60.0680,
            tipoComercio = "supermercado",
            categoria = TipoComercio.SUPERMERCADO.categoria,
            iconoEmoji = "🛒",
            realizaEnvios = true,
            cantidadOfertas = 3
        )
    )
}
