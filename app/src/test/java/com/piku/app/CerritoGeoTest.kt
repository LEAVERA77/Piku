package com.piku.app

import com.piku.app.data.CerritoGeo
import com.piku.app.data.ComerciosCerritoDemo
import com.piku.app.data.model.Comercio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CerritoGeoTest {

    private fun comercioApi(
        id: String = "real-1",
        nombre: String = "Kiosco Real",
        lat: Double? = -31.5800,
        lon: Double? = -60.0600
    ) = Comercio(id = id, nombre = nombre, lat = lat, lon = lon)

    @Test
    fun `los comercios reales de la API nunca desaparecen del mapa`() {
        val api = listOf(
            comercioApi(id = "real-1", nombre = "Kiosco Real"),
            comercioApi(id = "real-2", nombre = "Heladería Nueva", lat = -31.5810, lon = -60.0610)
        )

        val resultado = CerritoGeo.listaMapaCerrito(api)

        assertTrue(resultado.any { it.id == "real-1" })
        assertTrue(resultado.any { it.id == "real-2" })
    }

    @Test
    fun `el demo solo completa comercios que la API no devuelve`() {
        val demoNombre = ComerciosCerritoDemo.lista.first().nombre
        val api = listOf(comercioApi(id = "real-99", nombre = demoNombre))

        val resultado = CerritoGeo.listaMapaCerrito(api)

        // El comercio real con el mismo nombre reemplaza al demo (sin duplicados)
        val coincidencias = resultado.filter { it.nombre.equals(demoNombre, ignoreCase = true) }
        assertEquals(1, coincidencias.size)
        assertEquals("real-99", coincidencias.first().id)
        // El resto del catálogo demo sigue disponible
        assertTrue(resultado.any { it.esDemo() })
    }

    @Test
    fun `con la API vacia se mantiene el catalogo demo completo`() {
        val resultado = CerritoGeo.listaMapaCerrito(emptyList())
        assertEquals(ComerciosCerritoDemo.lista.size, resultado.size)
        assertTrue(resultado.all { it.esDemo() })
    }

    @Test
    fun `lat lon invertidos se corrigen para que el pin caiga en Cerrito`() {
        // lat/lon vienen intercambiados desde la BD
        val api = listOf(comercioApi(id = "real-3", nombre = "Verdulería", lat = -60.0667, lon = -31.5833))

        val resultado = CerritoGeo.listaMapaCerrito(api)
        val corregido = resultado.first { it.id == "real-3" }

        assertEquals(-31.5833, corregido.lat!!, 0.0001)
        assertEquals(-60.0667, corregido.lon!!, 0.0001)
        assertTrue(CerritoGeo.enZonaCerrito(corregido.lat!!, corregido.lon!!))
    }

    @Test
    fun `comercios sin coordenadas ni demo equivalente quedan fuera del mapa`() {
        val api = listOf(comercioApi(id = "real-4", nombre = "Sin Ubicación", lat = null, lon = null))
        val resultado = CerritoGeo.listaMapaCerrito(api)
        assertFalse(resultado.any { it.id == "real-4" })
    }

    @Test
    fun `conDistanciaDesde ordena por cercania`() {
        val lista = listOf(
            comercioApi(id = "lejos", nombre = "Lejos", lat = -31.60, lon = -60.10),
            comercioApi(id = "cerca", nombre = "Cerca", lat = -31.5834, lon = -60.0668)
        )
        val resultado = CerritoGeo.conDistanciaDesde(CerritoGeo.CENTRO_LAT, CerritoGeo.CENTRO_LON, lista)
        assertEquals("cerca", resultado.first().id)
        assertTrue(resultado.first().distanciaMetros!! < resultado.last().distanciaMetros!!)
    }
}
