package com.piku.app.data.nominatim

import com.google.gson.annotations.SerializedName

data class NominatimResult(
    val lat: String,
    val lon: String,
    @SerializedName("display_name") val displayName: String,
    val type: String? = null,
    val address: NominatimAddress? = null
)

data class NominatimReverseResult(
    @SerializedName("display_name") val displayName: String? = null,
    val address: NominatimAddress? = null,
    val lat: String? = null,
    val lon: String? = null
)
