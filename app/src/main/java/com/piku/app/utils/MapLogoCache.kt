package com.piku.app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MapLogoCache {

    private val memory = LruCache<String, Bitmap>(72)

    suspend fun load(context: Context, url: String?, sizePx: Int): Bitmap? {
        val resolved = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
        memory.get(resolved)?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(resolved)
                    .size(sizePx)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request)
                if (result !is SuccessResult) return@withContext null
                val drawable = result.drawable
                val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
                Canvas(bmp).apply {
                    drawable.setBounds(0, 0, sizePx, sizePx)
                    drawable.draw(this)
                }
                memory.put(resolved, bmp)
                bmp
            } catch (_: Exception) {
                null
            }
        }
    }
}
