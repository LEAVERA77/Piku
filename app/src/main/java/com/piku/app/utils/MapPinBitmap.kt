package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache

/**
 * Marcador tipo “gota” (estilo [ic_map_pin]): cuerpo de color, círculo blanco con emoji y badges.
 */
object MapPinBitmap {

    private const val PIN_W = 76
    private const val PIN_H = 96
    private const val LABEL_H = 20

    private val bitmapCache = LruCache<String, Bitmap>(96)

    /** Path del vector ic_map_pin (viewport 32×40). */
    private val TEARDROP = Path().apply {
        moveTo(16f, 0f)
        cubicTo(9.37f, 0f, 4f, 5.37f, 4f, 12f)
        cubicTo(4f, 21f, 16f, 40f, 16f, 40f)
        cubicTo(16f, 40f, 28f, 21f, 28f, 12f)
        cubicTo(28f, 5.37f, 22.63f, 0f, 16f, 0f)
        close()
    }

    fun crear(
        context: Context,
        emoji: String,
        nombre: String? = null,
        cantidadOfertas: Int = 0,
        ofertasNuevas: Int = 0,
        realizaEnvios: Boolean = false
    ): BitmapDrawable {
        val key = cacheKey(emoji, nombre, cantidadOfertas, ofertasNuevas, realizaEnvios)
        val bitmap = bitmapCache.get(key) ?: renderBitmap(
            emoji, nombre, cantidadOfertas, ofertasNuevas, realizaEnvios
        ).also { bitmapCache.put(key, it) }
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun cacheKey(
        emoji: String,
        nombre: String?,
        cantidadOfertas: Int,
        ofertasNuevas: Int,
        realizaEnvios: Boolean
    ): String = buildString {
        append(emoji)
        append('|')
        append(nombre.orEmpty())
        append('|')
        append(cantidadOfertas)
        append('|')
        append(ofertasNuevas)
        append('|')
        append(realizaEnvios)
    }

    private fun renderBitmap(
        emoji: String,
        nombre: String?,
        cantidadOfertas: Int,
        ofertasNuevas: Int,
        realizaEnvios: Boolean
    ): Bitmap {
        val emojiPin = if (realizaEnvios) "$emoji🚲" else emoji
        val mostrarNombre = !nombre.isNullOrBlank()
        val altoTotal = PIN_H + if (mostrarNombre) LABEL_H else 0

        val bitmap = Bitmap.createBitmap(PIN_W, altoTotal, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val pinColor = when {
            ofertasNuevas > 0 -> Color.parseColor("#7C4DFF")
            cantidadOfertas > 0 -> Color.parseColor("#FF6B35")
            else -> Color.parseColor("#00A86B")
        }

        val matrix = Matrix().apply {
            setScale(PIN_W / 32f, PIN_H / 40f)
        }
        val pinPath = Path().apply { addPath(TEARDROP, matrix) }

        // Sombra suave
        val sombra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(55, 0, 0, 0)
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.translate(2f, 3f)
        canvas.drawPath(pinPath, sombra)
        canvas.restore()

        val cuerpo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        }
        canvas.drawPath(pinPath, cuerpo)

        val borde = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawPath(pinPath, borde)

        // Círculo blanco (como ic_map_pin)
        val cx = PIN_W / 2f
        val cy = PIN_H * 0.28f
        val radio = PIN_W * 0.22f
        canvas.drawCircle(cx, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        })
        canvas.drawCircle(cx, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        })

        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = if (realizaEnvios) PIN_W * 0.22f else PIN_W * 0.28f
        }
        canvas.drawText(emojiPin, cx, cy + emojiPaint.textSize * 0.36f, emojiPaint)

        if (ofertasNuevas > 0) {
            dibujarBadge(canvas, cx + radio * 0.85f, cy - radio * 0.75f, "N", Color.parseColor("#7C4DFF"))
        } else if (cantidadOfertas > 0) {
            val label = if (cantidadOfertas > 9) "9+" else cantidadOfertas.toString()
            dibujarBadge(canvas, PIN_W - 14f, 12f, label, Color.parseColor("#FF6B35"))
        }

        if (mostrarNombre) {
            val nombrePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#263238")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val corto = if (nombre!!.length > 14) nombre.take(13) + "…" else nombre
            val fondoLabel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(230, 255, 255, 255)
                style = Paint.Style.FILL
            }
            val tw = nombrePaint.measureText(corto)
            canvas.drawRoundRect(
                cx - tw / 2f - 6f,
                PIN_H + 2f,
                cx + tw / 2f + 6f,
                PIN_H + LABEL_H - 2f,
                6f,
                6f,
                fondoLabel
            )
            canvas.drawText(corto, cx, PIN_H + LABEL_H - 6f, nombrePaint)
        }

        return bitmap
    }

    fun anchorY(nombre: String?): Float {
        val mostrarNombre = !nombre.isNullOrBlank()
        return if (mostrarNombre) PIN_H.toFloat() / (PIN_H + LABEL_H) else 1f
    }

    private fun dibujarBadge(canvas: Canvas, x: Float, y: Float, texto: String, color: Int) {
        val radio = if (texto.length > 1) 13f else 11f
        canvas.drawCircle(x, y, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        })
        canvas.drawCircle(x, y, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 2f
        })
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = if (texto.length > 1) 11f else 12f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(texto, x, y + tp.textSize * 0.35f, tp)
    }
}
