package com.piku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.piku.app.ui.media.PikuImages

@Composable
fun PikuPhotoImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: Dp = 0.dp
) {
    val shape = if (cornerRadius > 0.dp) RoundedCornerShape(cornerRadius) else null
    val clippedModifier = if (shape != null) modifier.clip(shape) else modifier

    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = clippedModifier,
        contentScale = contentScale,
        loading = {
            PhotoPlaceholder(modifier = Modifier.fillMaxSize())
        },
        error = {
            SubcomposeAsyncImage(
                model = PikuImages.regalo,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    )
}

@Composable
fun PhotoPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surface
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {}
}

@Composable
fun PikuPhotoOverlay(
    url: String,
    modifier: Modifier = Modifier,
    overlayAlpha: Float = 0.55f,
    contentScale: ContentScale = ContentScale.Crop
) {
    Box(modifier = modifier) {
        PikuPhotoImage(
            url = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = overlayAlpha))
        )
    }
}
