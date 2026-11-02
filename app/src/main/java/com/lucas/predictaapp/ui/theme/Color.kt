package com.lucas.predictaapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Los valores de una apariencia de la paleta "Atardecer". Hay dos completas, [LightPalette] y
 * [DarkPalette]; la oscura no es la clara invertida: el acento sube de luminosidad para leerse
 * sobre el fondo y la hero card arranca un paso más clara para no hundirse.
 */
@Immutable
data class PredictaPalette(
    val isDark: Boolean,
    val paper: Color,
    val card: Color,
    val ink: Color,
    val inkSecondary: Color,
    val inkTertiary: Color,
    val kicker: Color,
    val neutralSoft: Color,
    val separator: Color,
    val border: Color,
    /** Fondo de los botones translúcidos del header, sobre el velo. */
    val glass: Color,
    val glassDisabled: Color,
    val accent: Color,
    val accentSoft: Color,
    /** Texto/ícono sobre un relleno de [accent]. */
    val onAccent: Color,
    val accentEdge: Color,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val honey: Color,
    val onHoney: Color,
    val success: Color,
    val successSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerDeep: Color,
    val warn: Color,
    val warnSoft: Color,
    val inkStrong: Color,
    val washTop: Color,
    val washMid: Color,
    val blue: Color, val blueSoft: Color,
    val purple: Color, val purpleSoft: Color,
    val pink: Color, val pinkSoft: Color,
    val teal: Color, val tealSoft: Color,
)

/** Contrastes medidos (WCAG 2): ink/paper 16+, inkSecondary/card 5.68, inkTertiary/card 4.82, accent/card 6.80, blanco/heroStart 6.35. */
val LightPalette = PredictaPalette(
    isDark = false,
    paper = Color(0xFFFBF8F3),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF18161A),
    inkSecondary = Color(0xFF6B6660),
    inkTertiary = Color(0xFF77716A),
    kicker = Color(0xFF4A4540),
    neutralSoft = Color(0xFFF2EFEA),
    separator = Color(0xFFF0ECE6),
    border = Color(0xFFE6E2DC),
    glass = Color(0xB3FFFFFF),
    glassDisabled = Color(0x59FFFFFF),
    accent = Color(0xFFA3360C),
    accentSoft = Color(0xFFFDEBDC),
    onAccent = Color(0xFFFFFFFF),
    accentEdge = Color(0xFFE9B48F),
    heroStart = Color(0xFFAD360B),
    heroMid = Color(0xFF8A2A0C),
    heroEnd = Color(0xFF5E1C0B),
    honey = Color(0xFFF5A524),
    onHoney = Color(0xFF2A1600),
    success = Color(0xFF1E7A4A),
    successSoft = Color(0xFFE3F4EA),
    danger = Color(0xFFB42318),
    dangerSoft = Color(0xFFFDECEA),
    dangerDeep = Color(0xFF8F1C13),
    warn = Color(0xFF8A5C06),
    warnSoft = Color(0xFFFCF3E1),
    inkStrong = Color(0xFF3A3632),
    washTop = Color(0xFFFFD3B0),
    washMid = Color(0xFFFFEBDA),
    blue = Color(0xFF2F5FC4), blueSoft = Color(0xFFE6EDFA),
    purple = Color(0xFF6B4BC2), purpleSoft = Color(0xFFEEE9FA),
    pink = Color(0xFFB8336F), pinkSoft = Color(0xFFFBE8F1),
    teal = Color(0xFF1F7A70), tealSoft = Color(0xFFE2F3F0),
)

/**
 * Cacao, no negro: fondo marrón casi negro y tarjetas un paso más claras (1.15:1 entre sí).
 * Contrastes medidos: ink/card 13.96, inkSecondary/card 7.42, inkTertiary/card 5.64,
 * accent/card 7.33, onAccent/accent 8.15, blanco/heroStart 5.18, success/card 8.26.
 */
val DarkPalette = PredictaPalette(
    isDark = true,
    paper = Color(0xFF1A1310),
    card = Color(0xFF2A201B),
    ink = Color(0xFFF6EFE9),
    inkSecondary = Color(0xFFBBAFA6),
    inkTertiary = Color(0xFFA39890),
    kicker = Color(0xFFD2C4B9),
    neutralSoft = Color(0xFF30251F),
    separator = Color(0xFF382C24),
    border = Color(0xFF45372E),
    glass = Color(0x14FFFFFF),
    glassDisabled = Color(0x0AFFFFFF),
    accent = Color(0xFFFF9466),
    accentSoft = Color(0xFF3D2419),
    onAccent = Color(0xFF2A1206),
    accentEdge = Color(0xFF9C6B4C),
    heroStart = Color(0xFFC2410C),
    heroMid = Color(0xFF9A3412),
    heroEnd = Color(0xFF6B210C),
    honey = Color(0xFFF5A524),
    onHoney = Color(0xFF2A1600),
    success = Color(0xFF5FD08F),
    successSoft = Color(0xFF173222),
    danger = Color(0xFFFF8A7A),
    dangerSoft = Color(0xFF3D1C17),
    dangerDeep = Color(0xFFFFB4A8),
    warn = Color(0xFFF2C062),
    warnSoft = Color(0xFF382C12),
    inkStrong = Color(0xFFE4DAD2),
    washTop = Color(0xFF3B2014),
    washMid = Color(0xFF26180F),
    blue = Color(0xFF8FB0F5), blueSoft = Color(0xFF1C2638),
    purple = Color(0xFFB9A3F0), purpleSoft = Color(0xFF261F38),
    pink = Color(0xFFF29AC2), pinkSoft = Color(0xFF3A1C2A),
    teal = Color(0xFF7FD6CA), tealSoft = Color(0xFF15302C),
)

/**
 * Punto de acceso a la paleta activa. [PredictaTheme] fija [palette] según el modo del sistema;
 * como es estado de snapshot, todo lo que lee un color —composición, `drawBehind`, lambdas—
 * se vuelve a dibujar cuando cambia la apariencia, sin tener que pasar la paleta por parámetro.
 */
object PredictaColors {
    var palette: PredictaPalette by mutableStateOf(LightPalette)
        internal set

    val isDark: Boolean get() = palette.isDark

    // ── Roles ───────────────────────────────────────────────────────────────
    val paper: Color get() = palette.paper
    val card: Color get() = palette.card
    val ink: Color get() = palette.ink
    val inkSecondary: Color get() = palette.inkSecondary
    val inkTertiary: Color get() = palette.inkTertiary
    val kicker: Color get() = palette.kicker
    val neutralSoft: Color get() = palette.neutralSoft
    val separator: Color get() = palette.separator
    val border: Color get() = palette.border
    val glass: Color get() = palette.glass
    val glassDisabled: Color get() = palette.glassDisabled

    val accent: Color get() = palette.accent
    val accentSoft: Color get() = palette.accentSoft
    val onAccent: Color get() = palette.onAccent
    /** Acento apagado: tercer color de rango en barras (después de heroStart y honey). */
    val accentEdge: Color get() = palette.accentEdge
    val heroStart: Color get() = palette.heroStart
    val heroMid: Color get() = palette.heroMid
    val heroEnd: Color get() = palette.heroEnd
    /** Ámbar de marca: el dado del logo y acentos puntuales (siempre con [onHoney]). */
    val honey: Color get() = palette.honey
    val onHoney: Color get() = palette.onHoney

    val success: Color get() = palette.success
    val successSoft: Color get() = palette.successSoft
    val danger: Color get() = palette.danger
    val dangerSoft: Color get() = palette.dangerSoft
    val warn: Color get() = palette.warn
    val warnSoft: Color get() = palette.warnSoft

    /** Velo de la página: tinte de marca arriba que se funde en el papel. */
    val pageWash: Brush
        get() = Brush.verticalGradient(
            0f to palette.washTop,
            0.20f to palette.washMid,
            0.42f to palette.paper,
            1f to palette.paper,
        )

    /** Velo de un sheet: más corto que el de página, porque el sheet es más bajo. */
    val sheetWash: Brush
        get() = Brush.verticalGradient(
            0f to palette.washMid,
            0.30f to palette.paper,
            1f to palette.paper,
        )

    /** El único bloque grande de color de la app. Texto encima: siempre blanco. */
    val heroBrush: Brush
        get() = Brush.linearGradient(listOf(palette.heroStart, palette.heroMid, palette.heroEnd))

    /** Relleno de acciones primarias (botón +). Ícono encima: blanco en ambos modos. */
    val primaryBrush: Brush
        get() = Brush.linearGradient(listOf(palette.heroStart, palette.heroEnd))

    val blue: Color get() = palette.blue
    val blueSoft: Color get() = palette.blueSoft
    val purple: Color get() = palette.purple
    val purpleSoft: Color get() = palette.purpleSoft
    val pink: Color get() = palette.pink
    val pinkSoft: Color get() = palette.pinkSoft
    val teal: Color get() = palette.teal
    val tealSoft: Color get() = palette.tealSoft
}
