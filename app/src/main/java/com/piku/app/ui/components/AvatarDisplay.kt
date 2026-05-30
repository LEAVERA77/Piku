package com.piku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piku.app.ui.media.PikuImages

@Composable
fun AvatarDisplay(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "Avatar"
) {
    val emoji = avatarUrl?.takeIf { it.startsWith("emoji:") }?.removePrefix("emoji:")
    Box(
        modifier = modifier.clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (emoji != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 36.sp)
            }
        } else {
            PikuPhotoImage(
                url = avatarUrl?.takeIf { it.isNotBlank() } ?: PikuImages.avatarDefault,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                cornerRadius = 999.dp,
                contentScale = ContentScale.Crop
            )
        }
    }
}
