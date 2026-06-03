package com.piku.app.data.network

import android.content.Context
import android.util.Log
import com.piku.app.data.config.ConfigLoader
import com.piku.app.data.datastore.AuthDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * WebSocket para el panel comercio: recibe avisos cuando hay un nuevo canje (NOTIFY → servidor).
 */
class ComercioRealtimeClient(
    context: Context,
    private val onNuevaNotificacion: () -> Unit
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private var webSocket: WebSocket? = null

    fun conectar() {
        scope.launch {
            val token = AuthDataStore.token(appContext)
            if (token.isNullOrBlank()) return@launch

            val base = ConfigLoader.apiBaseUrl(appContext).trimEnd('/')
            val wsBase = base
                .replace("https://", "wss://")
                .replace("http://", "ws://")
            val encoded = URLEncoder.encode(token, Charsets.UTF_8.name())
            val url = "$wsBase/ws/comercio?token=$encoded"

            desconectar()
            val request = Request.Builder().url(url).build()
            webSocket = client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (text.contains("\"type\":\"notification\"") ||
                            text.contains("\"type\": \"notification\"")
                        ) {
                            onNuevaNotificacion()
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Log.w(TAG, "WebSocket comercio: ${t.message}")
                    }
                }
            )
        }
    }

    fun desconectar() {
        webSocket?.close(1000, null)
        webSocket = null
    }

    companion object {
        private const val TAG = "ComercioRealtime"
    }
}
