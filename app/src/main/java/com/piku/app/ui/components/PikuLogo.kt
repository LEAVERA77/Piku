package com.piku.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.piku.app.ui.theme.AmarilloPiku
import com.piku.app.ui.theme.PikuTheme
import com.piku.app.ui.theme.CelestePiku
import com.piku.app.ui.theme.NaranjaPiku
import com.piku.app.ui.theme.VerdePiku

@Composable
fun PikuLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showTagline: Boolean = true,
    tagline: String = "Tus puntos, tus descuentos",
    onGradient: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 8.dp)
    ) {
        PikuLogoMark(size = if (compact) 56.dp else 88.dp)
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            LogoLetter('P', VerdePiku, rotate = -6f, compact = compact)
            LogoLetter('i', NaranjaPiku, rotate = 4f, compact = compact)
            LogoLetter('k', CelestePiku, rotate = -3f, compact = compact)
            LogoLetter('u', AmarilloPiku, rotate = 5f, compact = compact)
        }
        if (showTagline && !compact) {
            Text(
                text = tagline,
                style = MaterialTheme.typography.titleMedium,
                color = if (onGradient) Color.White.copy(alpha = 0.95f)
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PikuLogoMark(size: Dp = 88.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    listOf(VerdePiku, CelestePiku, NaranjaPiku)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(size * 0.18f)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Box(
                Modifier
                    .size(size * 0.26f)
                    .offset(y = (-size * 0.04f))
                    .clip(RoundedCornerShape(50))
                    .background(AmarilloPiku)
            )
            Box(
                Modifier
                    .size(size * 0.14f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f))
            )
        }
        Text(
            text = "★",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .rotate(12f),
            fontSize = (size.value * 0.22f).sp,
            color = Color.White
        )
    }
}

@Composable
private fun LogoLetter(
    char: Char,
    color: Color,
    rotate: Float,
    compact: Boolean
) {
    Text(
        text = char.toString(),
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .rotate(rotate),
        fontSize = if (compact) 32.sp else 48.sp,
        fontWeight = FontWeight.ExtraBold,
        color = color
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewPikuLogo() {
    PikuTheme {
        PikuLogo(showTagline = true)
    }
}
