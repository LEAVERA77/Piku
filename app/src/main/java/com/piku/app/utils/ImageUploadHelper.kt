package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUploadHelper {

    fun comprimirImagen(context: Context, uri: Uri, maxPx: Int = 1024): ByteArray {
        val orientation = leerOrientacionExif(context, uri)

        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("No se pudo leer la imagen")
        val decoded = BitmapFactory.decodeStream(input)
        input.close()

        val original = aplicarOrientacionExif(decoded, orientation)

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
        if (decoded !== original) decoded.recycle()
        original.recycle()
        return out.toByteArray()
    }

    private fun leerOrientacionExif(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        } catch (_: Exception) {
            ExifInterface.ORIENTATION_NORMAL
        }
    }

    private fun aplicarOrientacionExif(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
