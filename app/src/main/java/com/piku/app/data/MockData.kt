package com.piku.app.data

import com.piku.app.data.model.NivelUsuario
import com.piku.app.data.model.Recompensa
import com.piku.app.data.model.TipoTransaccion
import com.piku.app.data.model.Transaccion
import com.piku.app.data.model.Usuario

/** Datos de prueba hasta conectar el backend real. */
object MockData {

    const val PUNTOS_SALDO = 1250
    const val EQUIVALENCIA_DESCUENTO = 125

    val usuario = Usuario(
        id = "usr-001",
        nombre = "María García",
        email = "maria@ejemplo.com",
        puntos = PUNTOS_SALDO,
        nivel = NivelUsuario.desdePuntos(PUNTOS_SALDO)
    )

    val transacciones = listOf(
        Transaccion("t1", "Compra en Café Luna", 50, "Hoy, 10:30", TipoTransaccion.GANADO),
        Transaccion("t2", "Descuento 10% panadería", -200, "Ayer, 18:15", TipoTransaccion.CANJEADO),
        Transaccion("t3", "Compra en Farmacia Central", 120, "12 may, 14:00", TipoTransaccion.GANADO),
        Transaccion("t4", "Bono bienvenida", 300, "1 may, 09:00", TipoTransaccion.GANADO),
        Transaccion("t5", "Café gratis", -150, "28 abr, 11:20", TipoTransaccion.CANJEADO)
    )

    val recompensas = listOf(
        Recompensa("r1", "Café gratis", 150, "☕", "Un café de tu tamaño favorito"),
        Recompensa("r2", "10% en panadería", 200, "🥐", "Descuento en tu próxima compra"),
        Recompensa("r3", "$20 en restaurante", 500, "🍽️", "Vale para comidas"),
        Recompensa("r4", "Envío gratis", 350, "📦", "Sin costo de envío en tu pedido")
    )
}
