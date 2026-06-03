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
    primary = AzulPiku,
    onPrimary = TarjetaBlanca,
    primaryContainer = AzulPikuContainer,
    onPrimaryContainer = Color(0xFF1E3A8A),
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
    onPrimary = TextoPrincipalDark,
    primaryContainer = Color(0xFF1B3D32),
    onPrimaryContainer = Color(0xFFB8F5D8),
    secondary = NaranjaPikuDark,
    onSecondary = TextoPrincipalDark,
    secondaryContainer = Color(0xFF4A2C12),
    onSecondaryContainer = Color(0xFFFFDCC8),
    tertiary = VerdePiku,
    background = FondoPikuDark,
    onBackground = TextoPrincipalDark,
    surface = SuperficieDark,
    onSurface = TextoPrincipalDark,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0)
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
