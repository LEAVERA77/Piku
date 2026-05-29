package com.piku.app.data.network

import android.content.Context
import com.piku.app.data.config.ConfigLoader
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit configurado desde assets/config.json.
 */
object RetrofitInstance {

    private const val FALLBACK_BASE = "https://piku-324e.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var apiService: PikuApiService? = null

    fun init(context: Context) {
        val base = ConfigLoader.apiBaseUrl(context).let { url ->
            if (url.endsWith("/")) url else "$url/"
        }
        apiService = Retrofit.Builder()
            .baseUrl(base)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PikuApiService::class.java)
    }

    val api: PikuApiService
        get() = apiService ?: throw IllegalStateException(
            "RetrofitInstance.init(context) debe llamarse en MainActivity.onCreate"
        )

    /** Base URL activa (útil para depuración). */
    fun baseUrlOrFallback(context: Context): String {
        return try {
            val url = ConfigLoader.apiBaseUrl(context)
            if (url.endsWith("/")) url else "$url/"
        } catch (_: Exception) {
            FALLBACK_BASE
        }
    }
}
