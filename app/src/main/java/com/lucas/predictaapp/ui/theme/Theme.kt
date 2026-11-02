package com.lucas.predictaapp.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Elige la apariencia (sigue al sistema), la publica en [PredictaColors] y llena el esquema de
 * Material con ella. El esquema existe solo para que lo que sigue siendo de Material (ripple,
 * cursor y handles de selección, diálogos, date picker) no caiga en el violeta por defecto.
 */
@Composable
fun PredictaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    // El tema es la raíz: nadie leyó la paleta todavía en esta composición, así que escribirla acá
    // no provoca una escritura hacia atrás.
    if (PredictaColors.palette !== palette) PredictaColors.palette = palette
    val colorScheme = remember(palette) { palette.toMaterialScheme() }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            // Íconos del sistema oscuros sobre claro, claros sobre oscuro.
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

private fun PredictaPalette.toMaterialScheme(): ColorScheme =
    if (isDark) {
        darkColorScheme(
            primary = accent, onPrimary = onAccent,
            primaryContainer = accentSoft, onPrimaryContainer = ink,
            secondary = heroStart, onSecondary = Color.White,
            tertiary = honey, onTertiary = onHoney,
            background = paper, onBackground = ink,
            surface = card, onSurface = ink,
            surfaceVariant = neutralSoft, onSurfaceVariant = inkSecondary,
            surfaceContainerHigh = card, surfaceContainerHighest = neutralSoft,
            outline = border, outlineVariant = separator,
            error = danger, onError = onAccent,
            errorContainer = dangerSoft, onErrorContainer = dangerDeep,
        )
    } else {
        lightColorScheme(
            primary = accent, onPrimary = onAccent,
            primaryContainer = accentSoft, onPrimaryContainer = heroEnd,
            secondary = heroMid, onSecondary = Color.White,
            tertiary = honey, onTertiary = onHoney,
            background = paper, onBackground = ink,
            surface = card, onSurface = ink,
            surfaceVariant = neutralSoft, onSurfaceVariant = inkSecondary,
            surfaceContainerHigh = card, surfaceContainerHighest = neutralSoft,
            outline = border, outlineVariant = separator,
            error = danger, onError = Color.White,
            errorContainer = dangerSoft, onErrorContainer = dangerDeep,
        )
    }
