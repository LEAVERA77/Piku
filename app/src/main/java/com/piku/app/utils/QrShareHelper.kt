package com.piku.app.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object QrShareHelper {

    fun compartirQr(
        context: Context,
        bitmap: Bitmap,
        codigo: String,
        monto: Double,
        puntos: Int?,
        minutosValidez: Int?
    ) {
        val cacheDir = File(context.cacheDir, "qr_share").apply { mkdirs() }
        val file = File(cacheDir, "piku_qr_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val pts = puntos?.toString() ?: "—"
        val validez = minutosValidez?.let { "$it minutos" } ?: "15 minutos"
        val mensaje = """
            🛍️ *Compra en mi comercio — Piku*

            💵 *Monto:* $${monto}
            🎁 *Puntos:* $pts
            📅 *Válido:* $validez

            📲 *Código:* $codigo

            Escaneá el QR con la app Piku para sumar puntos.
        """.trimIndent()

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, mensaje)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val chooser = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, mensaje)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                "Compartir QR"
            )
            context.startActivity(chooser)
        }
    }
}
