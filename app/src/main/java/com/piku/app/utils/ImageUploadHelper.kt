package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUploadHelper {

    fun comprimirImagen(context: Context, uri: Uri, maxPx: Int = 1024): ByteArray {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("No se pudo leer la imagen")
        val original = BitmapFactory.decodeStream(input)
        input.close()

        val scale = minOf(
            1f,
            maxPx.toFloat() / max(original.width, original.height)
        )
        val w = (original.width * scale).toInt().coerceAtLeast(1)
        val h = (original.height * scale).toInt().coerceAtLeast(1)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, w, h, true)
        } else {
            original
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        if (scaled !== original) scaled.recycle()
        original.recycle()
        return out.toByteArray()
    }
}
