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
    onPrimaryContainer = Color(0xFF0E2D8F),
    secondary = NaranjaPiku,
    onSecondary = TarjetaBlanca,
    secondaryContainer = SecundarioContainer,
    onSecondaryContainer = Color(0xFF8A3A00),
    tertiary = AcentoVerdeClaro,
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = PrimarioContainer,
    onTertiaryContainer = Color(0xFF00513E),
    background = FondoPiku,
    onBackground = TextoPrincipal,
    surface = TarjetaBlanca,
    onSurface = TextoPrincipal,
    surfaceVariant = Color(0xFFE3E9F7),
    onSurfaceVariant = TextoSecundario
)

private val DarkColorScheme = darkColorScheme(
    primary = VerdePikuDark,
    onPrimary = Color(0xFF00391F),
    primaryContainer = Color(0xFF0B3D2E),
    onPrimaryContainer = Color(0xFFA9F5D0),
    secondary = NaranjaPikuDark,
    onSecondary = Color(0xFF4A2400),
    secondaryContainer = Color(0xFF5C3210),
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = CelestePiku,
    onTertiary = Color(0xFF00363F),
    background = FondoPikuDark,
    onBackground = TextoPrincipalDark,
    surface = SuperficieDark,
    onSurface = TextoPrincipalDark,
    surfaceVariant = Color(0xFF242B36),
    onSurfaceVariant = Color(0xFFAEB9CB)
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
