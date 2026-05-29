package com.piku.app.data.config

import android.content.Context
import android.util.Log
import org.json.JSONObject

/**
 * Lee configuración desde assets/config.json (no se sube a Git).
 */
object ConfigLoader {

    private const val TAG = "ConfigLoader"
    private const val ASSET_FILE = "config.json"
    private const val DEFAULT_API = "https://piku-324e.onrender.com"

    private var cache: JSONObject? = null

    fun load(context: Context): JSONObject? {
        cache?.let { return it }
        return try {
            val raw = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
            JSONObject(raw).also { cache = it }
        } catch (e: Exception) {
            Log.w(TAG, "Sin $ASSET_FILE; copiá config.example.json → config.json", e)
            null
        }
    }

    fun apiBaseUrl(context: Context): String {
        val api = load(context)?.optJSONObject("api")
        val url = api?.optString("baseUrl", "")?.trim()?.trimEnd('/')?.ifEmpty { null }
        return url ?: DEFAULT_API
    }

    fun cloudinaryCloudName(context: Context): String? =
        load(context)?.optJSONObject("cloudinary")
            ?.optString("cloudName", "")?.trim()?.ifEmpty { null }

    fun nominatimBaseUrl(context: Context): String? =
        load(context)?.optJSONObject("nominatim")
            ?.optString("baseUrl", "")?.trim()?.trimEnd('/')?.ifEmpty { null }

    fun googleWebClientId(context: Context): String? =
        load(context)?.optJSONObject("google")
            ?.optString("webClientId", "")?.trim()?.ifEmpty { null }

    fun appTagline(context: Context): String =
        load(context)?.optJSONObject("app")
            ?.optString("tagline", "Tus puntos, tus descuentos")
            ?: "Tus puntos, tus descuentos"
}
