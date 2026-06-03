package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import android.util.TypedValue
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Marcador tipo gota para el mapa: emoji, badges de novedades y etiqueta legible.
 */
object MapPinBitmap {

    private const val CACHE_VERSION = "v2"
    private val bitmapCache = LruCache<String, Bitmap>(96)

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
        val escala = escalaPantalla(context)
        val key = "$CACHE_VERSION|${escala}|$emoji|${nombre.orEmpty()}|$cantidadOfertas|$ofertasNuevas|$realizaEnvios"
        val bitmap = bitmapCache.get(key) ?: renderBitmap(
            context, escala, emoji, nombre, cantidadOfertas, ofertasNuevas, realizaEnvios
        ).also { bitmapCache.put(key, it) }
        return BitmapDrawable(context.resources, bitmap)
    }

    fun anchorY(nombre: String?, ofertasNuevas: Int = 0, cantidadOfertas: Int = 0): Float {
        val pinH = 64f
        val labelH = if (nombre.isNullOrBlank()) {
            0f
        } else if (ofertasNuevas > 0 || cantidadOfertas > 0) {
            38f
        } else {
            28f
        }
        val total = pinH + labelH
        return if (total <= 0f) 1f else pinH / total
    }

    private fun escalaPantalla(context: Context): Float {
        val density = context.resources.displayMetrics.density
        return max(2f, density * 1.35f)
    }

    private fun sp(context: Context, sp: Float, escala: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        ) * (escala / context.resources.displayMetrics.density)

    private fun alturaEtiqueta(
        nombre: String?,
        ofertasNuevas: Int,
        cantidadOfertas: Int,
        escala: Float
    ): Int {
        if (nombre.isNullOrBlank()) return 0
        val tieneSub = ofertasNuevas > 0 || cantidadOfertas > 0
        return ((if (tieneSub) 38f else 28f) * escala).roundToInt()
    }

    private fun renderBitmap(
        context: Context,
        escala: Float,
        emoji: String,
        nombre: String?,
        cantidadOfertas: Int,
        ofertasNuevas: Int,
        realizaEnvios: Boolean
    ): Bitmap {
        val pinW = (52f * escala).roundToInt()
        val pinH = (64f * escala).roundToInt()
        val labelH = alturaEtiqueta(nombre, ofertasNuevas, cantidadOfertas, escala)

        val nombrePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A237E")
            textSize = sp(context, 13f, escala)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#546E7A")
            textSize = sp(context, 11f, escala)
            textAlign = Paint.Align.CENTER
        }

        val nombreCorto = nombre?.let {
            if (it.length > 22) it.take(21) + "…" else it
        }.orEmpty()
        val subTexto = textoNovedades(ofertasNuevas, cantidadOfertas)
        val anchoNombre = if (nombreCorto.isNotEmpty()) nombrePaint.measureText(nombreCorto) else 0f
        val anchoSub = if (subTexto.isNotEmpty()) subPaint.measureText(subTexto) else 0f
        val anchoMin = maxOf(
            pinW.toFloat(),
            max(anchoNombre, anchoSub) + 24f * escala
        ).roundToInt()
        val altoTotal = pinH + labelH

        val bitmap = Bitmap.createBitmap(anchoMin, altoTotal, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = anchoMin / 2f

        val pinColor = when {
            ofertasNuevas > 0 -> Color.parseColor("#7C4DFF")
            cantidadOfertas > 0 -> Color.parseColor("#FF6B35")
            else -> Color.parseColor("#00A86B")
        }

        val offsetX = (anchoMin - pinW) / 2f
        canvas.save()
        canvas.translate(offsetX, 0f)

        val matrix = Matrix().apply { setScale(pinW / 32f, pinH / 40f) }
        val pinPath = Path().apply { addPath(TEARDROP, matrix) }

        canvas.save()
        canvas.translate(2f * escala, 3f * escala)
        canvas.drawPath(
            pinPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(50, 0, 0, 0)
                style = Paint.Style.FILL
            }
        )
        canvas.restore()

        canvas.drawPath(pinPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinColor
            style = Paint.Style.FILL
        })
        canvas.drawPath(pinPath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(100, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f * escala
        })

        val cy = pinH * 0.28f
        val radio = pinW * 0.24f
        canvas.drawCircle(cx - offsetX, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        })

        val emojiSolo = emoji.trim()
        val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = pinW * 0.34f
        }
        canvas.drawText(
            emojiSolo,
            cx - offsetX,
            cy + emojiPaint.textSize * 0.38f,
            emojiPaint
        )

        if (realizaEnvios) {
            dibujarBadge(
                canvas,
                cx - offsetX + radio * 0.9f,
                cy + radio * 0.85f,
                "🚲",
                Color.parseColor("#0288D1"),
                escala,
                esEmoji = true
            )
        }

        when {
            ofertasNuevas > 0 -> {
                val txt = if (ofertasNuevas > 1) "$ofertasNuevas N" else "NUEVO"
                dibujarBadge(
                    canvas,
                    pinW - 10f * escala,
                    12f * escala,
                    txt,
                    Color.parseColor("#7C4DFF"),
                    escala
                )
            }
            cantidadOfertas > 0 -> {
                val txt = if (cantidadOfertas > 9) "9+" else cantidadOfertas.toString()
                dibujarBadge(
                    canvas,
                    pinW - 10f * escala,
                    12f * escala,
                    txt,
                    Color.parseColor("#FF6B35"),
                    escala
                )
            }
        }

        canvas.restore()

        if (labelH > 0 && nombreCorto.isNotEmpty()) {
            val padH = 8f * escala
            val padV = 6f * escala
            val tw = max(anchoNombre, anchoSub)
            val left = cx - tw / 2f - padH
            val top = pinH + 4f * escala
            val right = cx + tw / 2f + padH
            val bottom = pinH + labelH - 4f * escala

            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                10f * escala,
                10f * escala,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
            )
            canvas.drawRoundRect(
                RectF(left, top, right, bottom),
                10f * escala,
                10f * escala,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(90, 0, 0, 0)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.5f * escala
                }
            )

            val yNombre = top + padV + nombrePaint.textSize
            canvas.drawText(nombreCorto, cx, yNombre, nombrePaint)
            if (subTexto.isNotEmpty()) {
                canvas.drawText(subTexto, cx, yNombre + subPaint.textSize + 4f * escala, subPaint)
            }
        }

        return bitmap
    }

    private fun textoNovedades(ofertasNuevas: Int, cantidadOfertas: Int): String = when {
        ofertasNuevas > 0 && cantidadOfertas > 0 ->
            "✨ $ofertasNuevas nueva(s) · $cantidadOfertas oferta(s)"
        ofertasNuevas > 0 -> "✨ $ofertasNuevas oferta(s) nueva(s)"
        cantidadOfertas > 0 -> "🔥 $cantidadOfertas oferta(s) activa(s)"
        else -> ""
    }

    private fun dibujarBadge(
        canvas: Canvas,
        x: Float,
        y: Float,
        texto: String,
        color: Int,
        escala: Float,
        esEmoji: Boolean = false
    ) {
        if (esEmoji) {
            val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = 14f * escala
            }
            canvas.drawText(texto, x, y, tp)
            return
        }

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = if (texto.length > 3) 9f * escala else 11f * escala
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val tw = tp.measureText(texto)
        val padX = 6f * escala
        val padY = 4f * escala
        val h = tp.textSize + padY * 2
        val w = tw + padX * 2
        canvas.drawRoundRect(
            RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f),
            h / 2f,
            h / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
        )
        canvas.drawText(texto, x, y + tp.textSize * 0.35f, tp)
    }
}
