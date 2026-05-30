package com.piku.app.data.nominatim

import com.google.gson.annotations.SerializedName

data class NominatimAddress(
    val road: String? = null,
    @SerializedName("house_number") val houseNumber: String? = null,
    val pedestrian: String? = null,
    val footway: String? = null,
    val residential: String? = null,
    val suburb: String? = null,
    val city: String? = null,
    @SerializedName("town") val town: String? = null,
    @SerializedName("village") val village: String? = null
)

object NominatimAddressFormatter {

    fun calleDesde(address: NominatimAddress?, displayName: String?): String? {
        val desdeAddress = address?.road
            ?: address?.pedestrian
            ?: address?.footway
            ?: address?.residential
        if (!desdeAddress.isNullOrBlank()) return desdeAddress.trim()
        return extraerCalleDeDisplayName(displayName)
    }

    fun etiquetaSugerencia(result: NominatimResult): String {
        val calle = calleDesde(result.address, result.displayName)
        if (calle.isNullOrBlank()) return acortarDisplay(result.displayName)
        val num = result.address?.houseNumber?.trim()
        return if (!num.isNullOrBlank()) "$calle $num" else calle
    }

    /** Solo calle para el campo de texto (sin ciudad ni país). */
    fun textoParaCampo(result: NominatimResult): String {
        val calle = calleDesde(result.address, result.displayName) ?: return ""
        val num = result.address?.houseNumber?.trim()
        return if (!num.isNullOrBlank()) "$calle $num" else calle
    }

    fun consultaGeocode(textoUsuario: String, address: NominatimAddress?): String {
        val t = textoUsuario.trim()
        if (t.isBlank()) return t
        val ciudad = address?.city ?: address?.town ?: address?.village ?: address?.suburb
        return if (!ciudad.isNullOrBlank() && !t.contains(ciudad, ignoreCase = true)) {
            "$t, $ciudad"
        } else {
            t
        }
    }

    private fun extraerCalleDeDisplayName(displayName: String?): String? {
        if (displayName.isNullOrBlank()) return null
        val partes = displayName.split(",").map { it.trim() }
        val candidata = partes.firstOrNull { p ->
            p.length >= 3 && !p.all { it.isDigit() || it == '-' || it == ' ' }
        } ?: partes.firstOrNull()
        return candidata?.take(80)
    }

    private fun acortarDisplay(displayName: String): String {
        val idx = displayName.indexOf(',')
        return if (idx > 0) displayName.substring(0, idx).trim() else displayName.take(60)
    }
}
