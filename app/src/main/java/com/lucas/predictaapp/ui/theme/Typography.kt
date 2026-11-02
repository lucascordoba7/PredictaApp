package com.lucas.predictaapp.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lucas.predictaapp.R

/**
 * Plus Jakarta Sans, autoalojada: un solo archivo variable (`res/font/plus_jakarta_sans.ttf`,
 * OFL 1.1 — licencia en `licenses/`) resuelto por peso con [FontVariation].
 */
@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: FontWeight) = Font(
    resId = R.font.plus_jakarta_sans,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val JakartaSans = FontFamily(
    jakarta(FontWeight.Normal),
    jakarta(FontWeight.Medium),
    jakarta(FontWeight.SemiBold),
    jakarta(FontWeight.Bold),
    jakarta(FontWeight.ExtraBold),
)

/**
 * Escala del rediseño: pesos altos y tracking negativo arriba, 12sp como piso de lectura.
 * Los nombres viejos (`scoreHero`, `monoCap`…) siguen existiendo para las pantallas que todavía
 * no se migraron; el código nuevo usa `display`, `title`, `heading`, `row*`, `kicker`.
 */
object PredictaTypography {
    // ── Escala nueva ────────────────────────────────────────────────────────
    /** El monto de la hero card. Es el número que la pantalla existe para mostrar. */
    val display = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 46.sp, lineHeight = 50.sp, letterSpacing = (-1.8).sp)
    /** "Hola, Lucas", título de pantalla. */
    val title = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.5).sp)
    /** Título de sección ("Próximos pagos"). */
    val heading = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.3).sp)
    val rowTitle = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, lineHeight = 20.sp)
    val rowSubtitle = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp)
    val rowAmount = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 21.sp, letterSpacing = (-0.3).sp)
    /** Rótulo chico en mayúsculas. El caller hace `uppercase()` para que TalkBack lea la palabra. */
    val kicker = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.7.sp)
    val label = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)
    val button = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, lineHeight = 20.sp)

    // ── Nombres históricos (migración) ─────────────────────────────────────
    val scoreHero = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, letterSpacing = (-1).sp)
    val titlePage = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, letterSpacing = (-0.6).sp)
    val section = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, letterSpacing = (-0.3).sp)
    val cardTitle = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, letterSpacing = (-0.2).sp)
    val body = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.Medium, fontSize = 15.sp)
    val bodyTight = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    val small = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.Medium, fontSize = 13.sp)
    val caption = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val monoCap = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.3.sp)
    val kpiInline = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, letterSpacing = (-0.5).sp)
    val heroAvailable = TextStyle(fontFamily = JakartaSans, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-1.4).sp)
    val heroSpend = display
}
