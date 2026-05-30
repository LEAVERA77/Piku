package com.piku.app.data.nominatim

import retrofit2.http.GET
import retrofit2.http.Query

interface NominatimApi {
    @GET("search")
    suspend fun geocode(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("viewbox") viewbox: String? = null,
        @Query("bounded") bounded: Int? = null,
        @Query("addressdetails") addressdetails: Int = 1
    ): List<NominatimResult>

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressdetails: Int = 1
    ): NominatimReverseResult
}
