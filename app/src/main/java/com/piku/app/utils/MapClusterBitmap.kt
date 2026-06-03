package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.TypedValue
import kotlin.math.roundToInt

object MapClusterBitmap {

    fun crear(
        context: Context,
        cantidad: Int,
        zoomScale: Float = 1f,
        modoOscuro: Boolean = false
    ): BitmapDrawable {
        val escala = zoomScale.coerceIn(0.5f, 1.25f)
        val size = (52f * escala).roundToInt()
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = size / 2f
        val cy = size / 2f
        val radio = size * 0.42f

        canvas.drawCircle(cx, cy + 2f, radio + 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
        })

        val fill = if (modoOscuro) Color.parseColor("#2563EB") else Color.parseColor("#0D9488")
        canvas.drawCircle(cx, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fill
            style = Paint.Style.FILL
        })
        canvas.drawCircle(cx, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(if (modoOscuro) 200 else 120, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * escala
        })

        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                14f * escala,
                context.resources.displayMetrics
            )
        }
        val texto = if (cantidad > 99) "99+" else cantidad.toString()
        canvas.drawText(texto, cx, cy + countPaint.textSize * 0.35f, countPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    fun crearConEtiqueta(
        context: Context,
        cantidad: Int,
        zoomScale: Float = 1f,
        modoOscuro: Boolean = false
    ): BitmapDrawable {
        val escala = zoomScale.coerceIn(0.5f, 1.25f)
        val pinSize = (48f * escala).roundToInt()
        val labelH = (22f * escala).roundToInt()
        val label = "$cantidad comercios"
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (modoOscuro) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                9f * escala,
                context.resources.displayMetrics
            )
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }
        val tw = labelPaint.measureText(label)
        val w = maxOf(pinSize, (tw + 16f * escala).roundToInt())
        val h = pinSize + labelH
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = w / 2f

        val icon = crear(context, cantidad, zoomScale, modoOscuro)
        icon.setBounds((w - pinSize) / 2, 0, (w + pinSize) / 2, pinSize)
        icon.draw(canvas)

        val top = pinSize + 2f * escala
        val rect = RectF(cx - tw / 2f - 8f * escala, top, cx + tw / 2f + 8f * escala, top + labelH - 2f)
        canvas.drawRoundRect(rect, 8f * escala, 8f * escala, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (modoOscuro) Color.parseColor("#1E293B") else Color.WHITE
            style = Paint.Style.FILL
        })
        canvas.drawText(label, cx, top + labelH * 0.72f, labelPaint.apply {
            textAlign = Paint.Align.CENTER
        })

        return BitmapDrawable(context.resources, bitmap)
    }

    fun anchorY(): Float = 0.48f
}
