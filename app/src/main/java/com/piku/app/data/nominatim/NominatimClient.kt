package com.piku.app.data.nominatim

import android.content.Context
import com.piku.app.data.config.ConfigLoader
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NominatimClient {

    private const val DEFAULT_BASE = "http://167.234.235.76:8080/"

    @Volatile
    private var api: NominatimApi? = null

    fun get(context: Context): NominatimApi {
        return api ?: synchronized(this) {
            api ?: build(context).also { api = it }
        }
    }

    private fun build(context: Context): NominatimApi {
        val base = ConfigLoader.nominatimBaseUrl(context)?.let { url ->
            if (url.endsWith("/")) url else "$url/"
        } ?: DEFAULT_BASE

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NominatimApi::class.java)
    }
}
