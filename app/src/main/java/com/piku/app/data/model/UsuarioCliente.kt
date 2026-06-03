package com.piku.app.data.model

import com.google.gson.annotations.SerializedName
import com.piku.app.ui.media.PikuImages
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class SaldoApiResponse(
    val puntos: Int = 0,
    val equivalenciaDescuento: Int? = null,
    val mensaje: String? = null
) {
    fun equivalencia(): Int = equivalenciaDescuento ?: (puntos / 10)
}

data class TransaccionApi(
    val id: String,
    val tipo: String,
    val puntos: Int,
    val descripcion: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("comercio_nombre") val comercioNombre: String? = null
) {
    fun toTransaccion(): Transaccion {
        val desc = descripcion?.takeIf { it.isNotBlank() }
            ?: comercioNombre?.let { "Movimiento en $it" }
            ?: "Movimiento de puntos"
        val tipoTx = if (tipo.equals("canjeado", ignoreCase = true)) {
            TipoTransaccion.CANJEADO
        } else {
            TipoTransaccion.GANADO
        }
        val pts = if (tipoTx == TipoTransaccion.CANJEADO && puntos > 0) -puntos else puntos
        return Transaccion(
            id = id,
            descripcion = desc,
            puntos = pts,
            fecha = formatearFecha(createdAt),
            tipo = tipoTx
        )
    }

    private fun formatearFecha(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(iso.replace(" ", "T").let { s ->
                if (s.endsWith("Z") || s.contains("+")) s else "${s}Z"
            })
            DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("es", "AR"))
                .withZone(ZoneId.systemDefault())
                .format(instant)
        } catch (_: Exception) {
            iso.take(16).replace('T', ' ')
        }
    }
}

data class HistorialResponse(
    val transacciones: List<TransaccionApi> = emptyList()
)

data class DesglosePuntosResponse(
    val saldo: Int = 0,
    val compras: Int = 0,
    val bonos: Int = 0,
    val canjes: Int = 0
)

data class RecompensaDisponible(
    val id: String,
    @SerializedName("comercio_id") val comercioId: String? = null,
    val nombre: String,
    val descripcion: String? = null,
    @SerializedName("puntos_requeridos") val puntosRequeridos: Int,
    val icono: String? = null,
    @SerializedName("imagen_url") val imagenUrl: String? = null,
    @SerializedName("comercio_nombre") val comercioNombre: String? = null,
    @SerializedName("puede_canjear") val puedeCanjear: Boolean? = null
) {
    fun toRecompensa(cloudName: String?): Recompensa {
        val descParts = listOfNotNull(comercioNombre, descripcion).filter { it.isNotBlank() }
        return Recompensa(
            id = id,
            nombre = nombre,
            puntosRequeridos = puntosRequeridos,
            imageUrl = PikuImages.resolve(imagenUrl, id, nombre, cloudName),
            descripcion = descParts.joinToString(" · ").ifBlank { nombre }
        )
    }
}

data class RecompensasDisponiblesResponse(
    val puntosDisponibles: Int = 0,
    val recompensas: List<RecompensaDisponible> = emptyList()
)
