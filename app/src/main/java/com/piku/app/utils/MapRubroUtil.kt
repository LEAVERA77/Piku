package com.piku.app.utils

import com.piku.app.data.TipoComercio
import com.piku.app.data.model.Comercio
import com.piku.app.data.model.Rubro

object MapRubroUtil {

    fun normalizar(valor: String?): String =
        valor?.trim()?.lowercase()?.replace("é", "e")?.replace("í", "i") ?: ""

    fun coincideRubro(comercio: Comercio, catalogo: List<Rubro>, seleccionados: Set<String>): Boolean {
        if (seleccionados.isEmpty()) return true
        val tipoCanon = TipoComercio.desdeId(comercio.tipoComercio ?: comercio.categoria)
        val cat = normalizar(comercio.categoria).ifBlank { normalizar(tipoCanon.categoria) }
        val tipoId = normalizar(comercio.tipoComercio).ifBlank { normalizar(tipoCanon.id) }
        val rubrosActivos = catalogo.filter { seleccionados.contains(it.id) }
        return rubrosActivos.any { rubro ->
            rubro.id == tipoId ||
                rubro.id == cat ||
                rubro.categorias.any { c ->
                    val norm = normalizar(c)
                    cat == norm || cat.contains(norm) || norm.contains(cat) ||
                        tipoId == norm || tipoId.contains(norm) || norm.contains(tipoId)
                }
        }
    }
}
