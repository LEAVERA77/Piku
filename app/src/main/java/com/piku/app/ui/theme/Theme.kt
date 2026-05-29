package com.piku.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VerdePiku,
    onPrimary = TarjetaBlanca,
    secondary = NaranjaPiku,
    onSecondary = TarjetaBlanca,
    tertiary = AcentoVerdeClaro,
    background = FondoPiku,
    onBackground = TextoPrincipal,
    surface = TarjetaBlanca,
    onSurface = TextoPrincipal,
    surfaceVariant = FondoPiku,
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

@Composable
fun PikuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
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
