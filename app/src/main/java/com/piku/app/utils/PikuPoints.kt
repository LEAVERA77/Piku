package com.piku.app.utils

import kotlin.math.floor
import kotlin.math.round

/**
 * Piku Points (PP): 1 PP por 1 USD gastado; 1 PP = 0.15 USD de descuento.
 */
object PikuPoints {
    const val PESOS_POR_DOLAR_DEFAULT = 1400.0
    const val VALOR_PUNTO_USD = 0.15
    const val TASA_REINTEGRO = 0.15

    fun puntosDesdeMontoArs(montoArs: Double, pesosPorDolar: Double = PESOS_POR_DOLAR_DEFAULT): Int {
        if (montoArs <= 0 || pesosPorDolar <= 0) return 0
        return floor(montoArs / pesosPorDolar).toInt()
    }

    fun descuentoArs(puntos: Int, pesosPorDolar: Double = PESOS_POR_DOLAR_DEFAULT): Int {
        if (puntos <= 0 || pesosPorDolar <= 0) return 0
        return round(puntos * VALOR_PUNTO_USD * pesosPorDolar).toInt()
    }

    fun descuentoUsd(puntos: Int): Double {
        if (puntos <= 0) return 0.0
        return round(puntos * VALOR_PUNTO_USD * 100) / 100.0
    }
}
