package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable

object MapPinBitmap {

    fun crear(
        context: Context,
        emoji: String,
        nombre: String? = null,
        cantidadOfertas: Int = 0,
        ofertasNuevas: Int = 0,
        realizaEnvios: Boolean = false
    ): BitmapDrawable {
        val ancho = 96
        val alto = if (nombre.isNullOrBlank()) 72 else 96
        val emojiPin = if (realizaEnvios) "$emoji🚲" else emoji
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fondo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                ofertasNuevas > 0 -> Color.parseColor("#7C4DFF")
                cantidadOfertas > 0 -> Color.parseColor("#FF6B35")
                else -> Color.parseColor("#00A86B")
            }
            style = Paint.Style.STROKE
            strokeWidth = if (ofertasNuevas > 0) 5f else 4f
        }
        val rect = RectF(6f, 6f, ancho - 6f, alto - 6f)
        canvas.drawRoundRect(rect, 16f, 16f, fondo)
        canvas.drawRoundRect(rect, 16f, 16f, borde)

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
        }
        val emojiSize = if (realizaEnvios) 24f else 32f
        emojiPaint.textSize = emojiSize
        canvas.drawText(emojiPin, ancho / 2f, 44f, emojiPaint)

        if (!nombre.isNullOrBlank()) {
            val nombrePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#263238")
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val corto = if (nombre.length > 12) nombre.take(11) + "…" else nombre
            canvas.drawText(corto, ancho / 2f, 68f, nombrePaint)
        }

        if (ofertasNuevas > 0) {
            val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#7C4DFF")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(14f, 14f, 12f, badge)
            val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("N", 14f, 18f, texto)
        } else if (cantidadOfertas > 0) {
            val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6B35")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(ancho - 14f, 14f, 12f, badge)
            val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            val label = if (cantidadOfertas > 9) "9+" else cantidadOfertas.toString()
            canvas.drawText(label, ancho - 14f, 18f, texto)
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}
