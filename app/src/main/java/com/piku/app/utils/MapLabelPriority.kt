package com.piku.app.utils

import com.piku.app.data.model.Comercio

object MapLabelPriority {

    /** En zoom medio solo las etiquetas más cercanas al usuario. */
    fun idsConEtiqueta(comercios: List<Comercio>, zoom: Double): Set<String> {
        if (zoom < 15.4) return emptySet()
        if (zoom >= 16.8) return comercios.map { it.id }.toSet()
        val limite = when {
            zoom < 15.8 -> 1
            zoom < 16.2 -> 3
            else -> 5
        }
        return comercios
            .sortedBy { it.distanciaMetros ?: Int.MAX_VALUE }
            .take(limite)
            .map { it.id }
            .toSet()
    }
}
