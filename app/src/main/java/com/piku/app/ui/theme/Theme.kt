package com.piku.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VerdePiku,
    onPrimary = TarjetaBlanca,
    primaryContainer = PrimarioContainer,
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = NaranjaPiku,
    onSecondary = TarjetaBlanca,
    secondaryContainer = SecundarioContainer,
    onSecondaryContainer = Color(0xFF7C2D12),
    tertiary = AcentoVerdeClaro,
    background = FondoPiku,
    onBackground = TextoPrincipal,
    surface = TarjetaBlanca,
    onSurface = TextoPrincipal,
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = TextoSecundario
)

private val DarkColorScheme = darkColorScheme(
    primary = VerdePikuDark,
    onPrimary = TextoPrincipal,
    secondary = NaranjaPikuDark,
    onSecondary = TextoPrincipal,
    tertiary = VerdePiku,
    background = FondoPikuDark,
    onBackground = TextoPrincipalDark,
    surface = SuperficieDark,
    onSurface = TextoPrincipalDark,
    surfaceVariant = SuperficieDark,
    onSurfaceVariant = TextoSecundario
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PikuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity() ?: return@SideEffect
            val window = activity.window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
