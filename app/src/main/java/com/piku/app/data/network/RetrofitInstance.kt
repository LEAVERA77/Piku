package com.piku.app.data.network

import android.content.Context
import com.piku.app.BuildConfig
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.datastore.AuthDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    private const val FALLBACK_BASE = "https://piku-324e.onrender.com/"

    @Volatile
    private var apiService: PikuApiService? = null

    private lateinit var appContext: Context

    fun init(context: Context) {
        if (apiService != null) return
        appContext = context.applicationContext

        val authInterceptor = Interceptor { chain ->
            val token = runBlocking { AuthDataStore.token(appContext) }
            val request = if (!token.isNullOrBlank()) {
                chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        val base = ConfigLoader.apiBaseUrl(appContext).let { url ->
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
            "RetrofitInstance no inicializado. Verificá PikuApplication."
        )
}
