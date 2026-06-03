package com.piku.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object CompartirLogro {

    private const val DOWNLOAD_URL = "https://piku.app/download"

    val hitosPuntos = listOf(100, 500, 1000)

    fun mensajeCanje(ofertaNombre: String): String =
        "¡Canjeé «$ofertaNombre» con Piku Points! 🎉"

    fun mensajeHito(puntos: Int): String = when (puntos) {
        100 -> "¡Alcancé 100 Piku Points! 🎯"
        500 -> "¡Llegué a 500 Piku Points! 🚀"
        1000 -> "¡1.000 Piku Points! Soy leyenda en mi ciudad 🏆"
        else -> "¡Tengo $puntos Piku Points acumulados! 💪"
    }

    fun mensajeSaldo(puntos: Int, equivalenciaArs: Int): String =
        "Tengo $puntos Piku Points (≈ $$equivalenciaArs de descuento) con Piku. " +
            "Sumá puntos en comercios de tu ciudad."

    /** Devuelve el hito más alto alcanzado que aún no se celebró. */
    fun hitoNuevo(puntosActuales: Int, ultimoCelebrado: Int): Int? =
        hitosPuntos.filter { puntosActuales >= it && it > ultimoCelebrado }.maxOrNull()

    fun compartirLogro(context: Context, mensaje: String, imagen: Bitmap? = null) {
        val texto = "$mensaje\n\nDescargá Piku: $DOWNLOAD_URL"
        if (imagen == null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir logro"))
            return
        }

        val cacheDir = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(cacheDir, "piku_logro_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            imagen.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_TEXT, texto)
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir logro"))
    }
}
