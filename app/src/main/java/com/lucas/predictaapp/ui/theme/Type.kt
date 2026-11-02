package com.lucas.predictaapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle

private val Defaults = Typography()

private fun TextStyle.jakarta() = copy(fontFamily = JakartaSans)

/**
 * La escala de Material con Plus Jakarta Sans, solo para los componentes de Material que siguen en
 * uso (diálogos, campos de texto, date picker). La UI propia usa [PredictaTypography].
 */
val Typography = Typography(
    displayLarge = Defaults.displayLarge.jakarta(),
    displayMedium = Defaults.displayMedium.jakarta(),
    displaySmall = Defaults.displaySmall.jakarta(),
    headlineLarge = Defaults.headlineLarge.jakarta(),
    headlineMedium = Defaults.headlineMedium.jakarta(),
    headlineSmall = Defaults.headlineSmall.jakarta(),
    titleLarge = Defaults.titleLarge.jakarta(),
    titleMedium = Defaults.titleMedium.jakarta(),
    titleSmall = Defaults.titleSmall.jakarta(),
    bodyLarge = Defaults.bodyLarge.jakarta(),
    bodyMedium = Defaults.bodyMedium.jakarta(),
    bodySmall = Defaults.bodySmall.jakarta(),
    labelLarge = Defaults.labelLarge.jakarta(),
    labelMedium = Defaults.labelMedium.jakarta(),
    labelSmall = Defaults.labelSmall.jakarta(),
)
