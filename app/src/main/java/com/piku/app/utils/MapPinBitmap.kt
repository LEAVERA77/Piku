package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import android.util.TypedValue
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Marcadores del mapa Piku: diseño moderno, escala con zoom y estados visuales por novedades.
 */
object MapPinBitmap {

    private const val CACHE_VERSION = "v5"
    private const val ZOOM_REF = 16.0
    private const val ZOOM_MIN = 14.0
    private const val ZOOM_MAX = 20.0

    private val bitmapCache = LruCache<String, Bitmap>(128)

    /** Escala visual según nivel de zoom OSM (más zoom = pin más grande). */
    fun escalaDesdeZoom(zoom: Double): Float {
        val t = ((zoom - ZOOM_MIN) / (ZOOM_MAX - ZOOM_MIN)).coerceIn(0.0, 1.0)
        return (0.55 + t * 0.65).toFloat()
    }

    /** Etiqueta con nombre solo cuando hay suficiente zoom para leerla. */
    fun mostrarEtiqueta(zoom: Double): Boolean = zoom >= 15.4

    private val TEARDROP = Path().apply {
        moveTo(16f, 0f)
        cubicTo(8.5f, 0f, 2f, 6.2f, 2f, 14f)
        cubicTo(2f, 24f, 16f, 42f, 16f, 42f)
        cubicTo(16f, 42f, 30f, 24f, 30f, 14f)
        cubicTo(30f, 6.2f, 23.5f, 0f, 16f, 0f)
        close()
    }

    private enum class PinEstado {
        NORMAL, OFERTAS, NOVEDAD, DESTACADO
    }

    private data class PinPalette(
        val top: Int,
        val bottom: Int,
        val accent: Int,
        val ring: Int,
        val badge: Int
    )

    private fun palette(estado: PinEstado): PinPalette = when (estado) {
        PinEstado.NORMAL -> PinPalette(
            top = Color.parseColor("#14B8A6"),
            bottom = Color.parseColor("#0D9488"),
            accent = Color.parseColor("#0F766E"),
            ring = Color.parseColor("#5EEAD4"),
            badge = Color.parseColor("#0D9488")
        )
        PinEstado.OFERTAS -> PinPalette(
            top = Color.parseColor("#FB923C"),
            bottom = Color.parseColor("#EA580C"),
            accent = Color.parseColor("#C2410C"),
            ring = Color.parseColor("#FDBA74"),
            badge = Color.parseColor("#EA580C")
        )
        PinEstado.NOVEDAD -> PinPalette(
            top = Color.parseColor("#A78BFA"),
            bottom = Color.parseColor("#7C3AED"),
            accent = Color.parseColor("#5B21B6"),
            ring = Color.parseColor("#C4B5FD"),
            badge = Color.parseColor("#7C3AED")
        )
        PinEstado.DESTACADO -> PinPalette(
            top = Color.parseColor("#FCD34D"),
            bottom = Color.parseColor("#F59E0B"),
            accent = Color.parseColor("#D97706"),
            ring = Color.parseColor("#FDE68A"),
            badge = Color.parseColor("#F59E0B")
        )
    }

    private fun resolverEstado(
        destacado: Boolean,
        ofertasNuevas: Int,
        cantidadOfertas: Int
    ): PinEstado = when {
        destacado -> PinEstado.DESTACADO
        ofertasNuevas > 0 -> PinEstado.NOVEDAD
        cantidadOfertas > 0 -> PinEstado.OFERTAS
        else -> PinEstado.NORMAL
    }

    fun crear(
        context: Context,
        emoji: String,
        nombre: String? = null,
        cantidadOfertas: Int = 0,
        ofertasNuevas: Int = 0,
        realizaEnvios: Boolean = false,
        destacado: Boolean = false,
        zoomScale: Float = 1f,
        mostrarEtiqueta: Boolean = true,
        modoOscuro: Boolean = false,
        atenuado: Boolean = false,
        pulsePhase: Float = -1f,
        logoBitmap: Bitmap? = null
    ): BitmapDrawable {
        val escala = escalaPantalla(context) * zoomScale.coerceIn(0.45f, 1.35f)
        val key = buildString {
            append(CACHE_VERSION)
            append('|')
            append("%.2f".format(escala))
            append('|')
            append(emoji)
            append('|')
            append(nombre.orEmpty())
            append('|')
            append(cantidadOfertas)
            append('|')
            append(ofertasNuevas)
            append('|')
            append(realizaEnvios)
            append('|')
            append(destacado)
            append('|')
            append(mostrarEtiqueta)
            append('|')
            append(modoOscuro)
            append('|')
            append(atenuado)
            append('|')
            append(if (pulsePhase >= 0f) (pulsePhase * 10).toInt() else -1)
            append('|')
            append(logoBitmap != null)
        }
        val bitmap = bitmapCache.get(key) ?: renderBitmap(
            context,
            escala,
            emoji,
            nombre,
            cantidadOfertas,
            ofertasNuevas,
            realizaEnvios,
            destacado,
            mostrarEtiqueta,
            modoOscuro,
            atenuado,
            pulsePhase,
            logoBitmap
        ).also { bitmapCache.put(key, it) }
        return BitmapDrawable(context.resources, bitmap)
    }

    fun anchorY(
        nombre: String?,
        ofertasNuevas: Int = 0,
        cantidadOfertas: Int = 0,
        zoomScale: Float = 1f,
        mostrarEtiqueta: Boolean = true
    ): Float {
        val pinH = 52f * zoomScale.coerceIn(0.45f, 1.35f)
        val labelH = if (mostrarEtiqueta && !nombre.isNullOrBlank()) {
            if (ofertasNuevas > 0 || cantidadOfertas > 0) 30f else 24f
        } else {
            0f
        } * zoomScale.coerceIn(0.45f, 1.35f)
        val total = pinH + labelH
        return if (total <= 0f) 1f else pinH / total
    }

    private fun escalaPantalla(context: Context): Float {
        val density = context.resources.displayMetrics.density
        return max(0.92f, density * 0.68f)
    }

    private fun sp(context: Context, sp: Float, escala: Float): Float =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp * escala.coerceIn(0.7f, 1.2f),
            context.resources.displayMetrics
        )

    private fun alturaEtiqueta(
        nombre: String?,
        ofertasNuevas: Int,
        cantidadOfertas: Int,
        escala: Float,
        mostrarEtiqueta: Boolean
    ): Int {
        if (!mostrarEtiqueta || nombre.isNullOrBlank()) return 0
        val tieneSub = ofertasNuevas > 0 || cantidadOfertas > 0
        return ((if (tieneSub) 30f else 24f) * escala).roundToInt()
    }

    private fun renderBitmap(
        context: Context,
        escala: Float,
        emoji: String,
        nombre: String?,
        cantidadOfertas: Int,
        ofertasNuevas: Int,
        realizaEnvios: Boolean,
        destacado: Boolean,
        mostrarEtiqueta: Boolean,
        modoOscuro: Boolean,
        atenuado: Boolean,
        pulsePhase: Float,
        logoBitmap: Bitmap?
    ): Bitmap {
        val estado = resolverEstado(destacado, ofertasNuevas, cantidadOfertas)
        val palette = palette(estado)

        val pinW = (42f * escala).roundToInt()
        val pinH = (52f * escala).roundToInt()
        val labelH = alturaEtiqueta(nombre, ofertasNuevas, cantidadOfertas, escala, mostrarEtiqueta)

        val labelTypeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        val nombrePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (modoOscuro) Color.parseColor("#F8FAFC") else Color.parseColor("#0F172A")
            textSize = sp(context, 10.5f, escala)
            typeface = labelTypeface
            textAlign = Paint.Align.LEFT
            letterSpacing = 0.01f
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (modoOscuro) Color.parseColor("#94A3B8") else Color.parseColor("#64748B")
            textSize = sp(context, 8.5f, escala)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }

        val nombreCorto = if (mostrarEtiqueta) {
            nombre?.let { if (it.length > 20) it.take(19) + "…" else it }.orEmpty()
        } else ""
        val subTexto = if (mostrarEtiqueta) textoNovedades(ofertasNuevas, cantidadOfertas) else ""

        val padLabelH = 10f * escala
        val padLabelV = 7f * escala
        val accentW = 3.5f * escala
        val anchoNombre = if (nombreCorto.isNotEmpty()) nombrePaint.measureText(nombreCorto) else 0f
        val anchoSub = if (subTexto.isNotEmpty()) subPaint.measureText(subTexto) else 0f
        val contenidoW = max(anchoNombre, anchoSub)
        val labelW = if (labelH > 0) (contenidoW + padLabelH * 2 + accentW).roundToInt() else 0
        val anchoMin = maxOf(pinW, labelW).coerceAtLeast(pinW)
        val altoTotal = pinH + labelH

        val bitmap = Bitmap.createBitmap(anchoMin, altoTotal, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val cx = anchoMin / 2f
        val offsetX = (anchoMin - pinW) / 2f

        // Halo / pulso animado para novedades
        if (estado == PinEstado.NOVEDAD) {
            val wave = if (pulsePhase >= 0f) {
                0.5f + 0.5f * sin(pulsePhase * 2.0 * Math.PI).toFloat()
            } else 0.35f
            val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb((45 + 70 * wave).toInt(), Color.red(palette.ring), Color.green(palette.ring), Color.blue(palette.ring))
                style = Paint.Style.FILL
            }
            canvas.drawCircle(cx, pinH * 0.22f, pinW * (0.58f + wave * 0.22f), haloPaint)
            haloPaint.color = Color.argb((25 + 45 * wave).toInt(), Color.red(palette.accent), Color.green(palette.accent), Color.blue(palette.accent))
            canvas.drawCircle(cx, pinH * 0.22f, pinW * (0.74f + wave * 0.16f), haloPaint)
        }

        canvas.save()
        canvas.translate(offsetX, 0f)

        val matrix = Matrix().apply { setScale(pinW / 32f, pinH / 42f) }
        val pinPath = Path().apply { addPath(TEARDROP, matrix) }

        // Sombra suave
        canvas.save()
        canvas.translate(2.5f * escala, 4f * escala)
        canvas.drawPath(
            pinPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(72, 15, 23, 42)
                style = Paint.Style.FILL
            }
        )
        canvas.restore()

        // Cuerpo con gradiente
        canvas.drawPath(
            pinPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, 0f, pinH.toFloat(),
                    palette.top, palette.bottom,
                    Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
            }
        )

        // Borde luminoso (más visible en modo oscuro)
        canvas.drawPath(
            pinPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(if (modoOscuro) 220 else 140, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = if (modoOscuro) 2.4f * escala else 1.8f * escala
            }
        )

        // Círculo para logo o emoji
        val cy = pinH * 0.26f
        val radio = pinW * 0.255f
        val iconCx = cx - offsetX
        canvas.drawCircle(iconCx, cy, radio + 1.2f * escala, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 0, 0, 0)
            style = Paint.Style.FILL
        })
        canvas.drawCircle(iconCx, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        })
        canvas.drawCircle(iconCx, cy, radio, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(if (modoOscuro) 80 else 50, 15, 23, 42)
            style = Paint.Style.STROKE
            strokeWidth = 1f * escala
        })

        val logoOk = logoBitmap != null && !logoBitmap.isRecycled
        if (logoOk) {
            val clip = Path().apply { addCircle(iconCx, cy, radio * 0.92f, Path.Direction.CW) }
            canvas.save()
            canvas.clipPath(clip)
            val dst = RectF(iconCx - radio * 0.92f, cy - radio * 0.92f, iconCx + radio * 0.92f, cy + radio * 0.92f)
            canvas.drawBitmap(logoBitmap!!, null, dst, Paint(Paint.ANTI_ALIAS_FLAG))
            canvas.restore()
        } else {
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                textSize = pinW * 0.36f
            }
            canvas.drawText(
                emoji.trim(),
                iconCx,
                cy + emojiPaint.textSize * 0.36f,
                emojiPaint
            )
        }

        if (realizaEnvios) {
            dibujarChipCircular(
                canvas,
                cx - offsetX + radio * 0.95f,
                cy + radio * 0.9f,
                "🚚",
                Color.parseColor("#0284C7"),
                escala
            )
        }

        if (destacado) {
            dibujarChipCircular(
                canvas,
                11f * escala,
                11f * escala,
                "★",
                palette.badge,
                escala,
                textoBlanco = true
            )
        }

        when {
            ofertasNuevas > 0 -> {
                val txt = if (ofertasNuevas > 1) "$ofertasNuevas" else "!"
                val wave = if (pulsePhase >= 0f) {
                    0.5f + 0.5f * sin(pulsePhase * 2.0 * Math.PI).toFloat()
                } else 0f
                dibujarBadge(
                    canvas,
                    pinW - 9f * escala,
                    10f * escala,
                    txt,
                    palette.badge,
                    escala,
                    pulso = true,
                    pulseWave = wave
                )
            }
            cantidadOfertas > 0 -> {
                val txt = if (cantidadOfertas > 9) "9+" else cantidadOfertas.toString()
                dibujarBadge(
                    canvas,
                    pinW - 9f * escala,
                    10f * escala,
                    txt,
                    palette.badge,
                    escala
                )
            }
        }

        canvas.restore()

        // Etiqueta flotante
        if (labelH > 0 && nombreCorto.isNotEmpty()) {
            val left = cx - labelW / 2f
            val top = pinH + 5f * escala
            val right = left + labelW
            val bottom = top + labelH - 6f * escala
            val rect = RectF(left, top, right, bottom)
            val radius = 12f * escala

            canvas.drawRoundRect(
                rect,
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(48, 15, 23, 42)
                    style = Paint.Style.FILL
                }
            )
            canvas.drawRoundRect(
                RectF(rect.left + 1f, rect.top + 2f, rect.right + 1f, rect.bottom + 2f),
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(38, 15, 23, 42)
                    style = Paint.Style.FILL
                }
            )
            canvas.drawRoundRect(
                rect,
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (modoOscuro) Color.parseColor("#1E293B") else Color.WHITE
                    style = Paint.Style.FILL
                }
            )
            canvas.drawRoundRect(
                rect,
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(if (modoOscuro) 60 else 28, 15, 23, 42)
                    style = Paint.Style.STROKE
                    strokeWidth = 1.2f * escala
                }
            )

            // Franja de color según estado
            canvas.drawRoundRect(
                RectF(rect.left, rect.top, rect.left + accentW, rect.bottom),
                radius,
                radius,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = palette.accent
                    style = Paint.Style.FILL
                }
            )

            val textX = rect.left + accentW + padLabelH
            val yNombre = rect.top + padLabelV + nombrePaint.textSize * 0.85f
            canvas.drawText(nombreCorto, textX, yNombre, nombrePaint)
            if (subTexto.isNotEmpty()) {
                canvas.drawText(subTexto, textX, yNombre + subPaint.textSize + 3f * escala, subPaint)
            }
        }

        if (atenuado) {
            val faded = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            Canvas(faded).drawBitmap(bitmap, 0f, 0f, Paint().apply { alpha = 102 })
            return faded
        }
        return bitmap
    }

    private fun textoNovedades(ofertasNuevas: Int, cantidadOfertas: Int): String = when {
        ofertasNuevas > 0 && cantidadOfertas > 0 ->
            "✨ $ofertasNuevas nueva(s) · $cantidadOfertas oferta(s)"
        ofertasNuevas > 0 -> "✨ $ofertasNuevas oferta(s) nueva(s)"
        cantidadOfertas > 0 -> "🎁 $cantidadOfertas oferta(s) activa(s)"
        else -> ""
    }

    private fun dibujarChipCircular(
        canvas: Canvas,
        x: Float,
        y: Float,
        texto: String,
        color: Int,
        escala: Float,
        textoBlanco: Boolean = false
    ) {
        val r = 9f * escala
        canvas.drawCircle(x, y, r + 1f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.argb(50, 0, 0, 0)
            style = Paint.Style.FILL
        })
        canvas.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        })
        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = if (texto.length == 1) 11f * escala else 10f * escala
            typeface = Typeface.DEFAULT_BOLD
            this.color = if (textoBlanco) Color.WHITE else Color.BLACK
        }
        canvas.drawText(texto, x, y + tp.textSize * 0.35f, tp)
    }

    private fun dibujarBadge(
        canvas: Canvas,
        x: Float,
        y: Float,
        texto: String,
        color: Int,
        escala: Float,
        pulso: Boolean = false,
        pulseWave: Float = 0f
    ) {
        if (pulso) {
            val r = 11f * escala + pulseWave * 4f * escala
            canvas.drawCircle(x, y, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb((40 + 80 * pulseWave).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                style = Paint.Style.FILL
            })
        }

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = if (texto.length > 2) 9f * escala else 11f * escala
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val tw = tp.measureText(texto)
        val padX = 5.5f * escala
        val padY = 3.5f * escala
        val h = tp.textSize + padY * 2
        val w = max(tw + padX * 2, h)
        canvas.drawRoundRect(
            RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f),
            h / 2f,
            h / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }
        )
        canvas.drawRoundRect(
            RectF(x - w / 2f, y - h / 2f, x + w / 2f, y + h / 2f),
            h / 2f,
            h / 2f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.argb(80, 255, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 1f * escala
            }
        )
        canvas.drawText(texto, x, y + tp.textSize * 0.35f, tp)
    }
}
