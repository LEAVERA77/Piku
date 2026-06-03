package com.piku.app.data

import com.piku.app.data.model.Comercio

/**
 * Comercios de prueba en Cerrito (Entre Ríos) cuando la API no devuelve datos.
 * Coordenadas alineadas con [seedComerciosCerrito.js].
 */
object ComerciosCerritoDemo {

    val lista: List<Comercio> = listOf(
        Comercio(
            id = "demo:cafe-martinez",
            nombre = "Café Martínez",
            direccion = "Belgrano 345, Cerrito, Entre Ríos",
            lat = -31.9189,
            lon = -60.6085,
            tipoComercio = "cafeteria",
            iconoEmoji = "☕",
            realizaEnvios = true,
            cantidadOfertas = 3
        ),
        Comercio(
            id = "demo:pizzeria-don-juan",
            nombre = "Pizzería Don Juan",
            direccion = "San Martín 890, Cerrito, Entre Ríos",
            lat = -31.9175,
            lon = -60.6078,
            tipoComercio = "restaurante",
            iconoEmoji = "🍽️",
            realizaEnvios = true,
            cantidadOfertas = 3
        ),
        Comercio(
            id = "demo:farmacia-cerrito",
            nombre = "Farmacia Cerrito",
            direccion = "25 de Mayo 123, Cerrito, Entre Ríos",
            lat = -31.9202,
            lon = -60.6092,
            tipoComercio = "farmacia",
            iconoEmoji = "💊",
            realizaEnvios = true,
            cantidadOfertas = 2
        ),
        Comercio(
            id = "demo:moda-urbana",
            nombre = "Moda Urbana",
            direccion = "Rivadavia 567, Cerrito, Entre Ríos",
            lat = -31.9168,
            lon = -60.6065,
            tipoComercio = "ropa",
            iconoEmoji = "👕",
            realizaEnvios = true,
            cantidadOfertas = 2
        ),
        Comercio(
            id = "demo:super-el-ahorro",
            nombre = "Supermercado El Ahorro",
            direccion = "Urquiza 789, Cerrito, Entre Ríos",
            lat = -31.9195,
            lon = -60.6101,
            tipoComercio = "supermercado",
            iconoEmoji = "🛒",
            realizaEnvios = true,
            cantidadOfertas = 3
        )
    )
}
