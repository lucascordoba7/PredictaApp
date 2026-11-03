package com.lucas.predictaapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lucas.predictaapp.ui.theme.PredictaColors
import com.lucas.predictaapp.ui.theme.PredictaDimensions
import com.lucas.predictaapp.ui.theme.PredictaTypography

/*
 * Piezas del lenguaje visual Atardecer. Todas las pantallas rediseñadas se arman con estas:
 * velo de página, una hero card con degradé, tarjetas blancas con sombra suave e insignias
 * de ícono sobre fondo de acento tenue.
 */

/** Fondo de pantalla: tinte de marca arriba que se funde en papel. */
fun Modifier.pageWash(): Modifier = background(PredictaColors.pageWash)

/** Sombra corta + sombra amplia muy tenue: separa la tarjeta del papel sin borde. */
fun Modifier.softShadow(shape: RoundedCornerShape, elevation: Dp = 10.dp): Modifier =
    shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = PredictaColors.ink.copy(alpha = 0.05f),
        spotColor = PredictaColors.ink.copy(alpha = 0.08f),
    )

/** El único bloque grande de color: degradé de marca, texto blanco, puntos del dado de fondo. */
@Composable
fun HeroCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(22.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(PredictaDimensions.Radius.hero)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(PredictaColors.heroBrush),
    ) {
        HeroDots()
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
private fun BoxScope.HeroDots() {
    val dot = Color.White.copy(alpha = 0.07f)
    Box(Modifier.align(Alignment.TopEnd).offset(x = 18.dp, y = (-18).dp).size(92.dp).clip(CircleShape).background(dot))
    Box(Modifier.align(Alignment.TopEnd).offset(x = (-54).dp, y = 60.dp).size(44.dp).clip(CircleShape).background(dot))
    Box(Modifier.align(Alignment.TopEnd).offset(x = (-18).dp, y = 118.dp).size(22.dp).clip(CircleShape).background(dot))
}

/** Tarjeta blanca estándar. */
@Composable
fun PredictaCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(PredictaDimensions.Radius.card)
    Column(
        modifier = modifier
            .softShadow(shape)
            .clip(shape)
            .background(PredictaColors.card)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content,
    )
}

/** Insignia cuadrada redondeada para íconos o emojis de categoría. */
@Composable
fun GlyphBadge(
    modifier: Modifier = Modifier,
    size: Dp = PredictaDimensions.Heights.badge,
    container: Color = PredictaColors.accentSoft,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(PredictaDimensions.Radius.badge))
            .background(container),
        content = content,
    )
}

/** Rótulo chico en mayúsculas ("NOVIEMBRE 2026", "FINANZAS"). */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = PredictaColors.kicker) {
    Text(text = text.uppercase(), style = PredictaTypography.kicker, color = color, modifier = modifier)
}

/** Título de sección con acción opcional a la derecha ("Ver todo"). */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = PredictaTypography.heading, color = PredictaColors.ink)
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                style = PredictaTypography.label,
                color = PredictaColors.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            )
        }
    }
}

/** Píldora de estado (texto sobre fondo tenue del mismo color). */
@Composable
fun StatusPill(text: String, container: Color, content: Color, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = PredictaTypography.caption.copy(color = content, fontWeight = PredictaTypography.label.fontWeight),
        modifier = modifier
            .clip(RoundedCornerShape(PredictaDimensions.Radius.pill))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * La marca: medio sol ámbar sobre el horizonte y su reflejo en tres líneas, sobre el
 * degradé de la hero. Mismo dibujo que el ícono de la app (grilla de 108, como el adaptive icon).
 */
@Composable
fun SunMark(modifier: Modifier = Modifier, size: Dp = 34.dp) {
    val sun = PredictaColors.honey
    Canvas(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(PredictaColors.heroBrush),
    ) {
        val u = this.size.width / 108f
        drawArc(
            color = sun,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(31 * u, 28 * u),
            size = Size(46 * u, 46 * u),
        )
        // (desde, hasta, alfa): el reflejo se acorta y se apaga hacia abajo.
        listOf(Triple(31.5f, 76.5f, 0.92f), Triple(39.5f, 68.5f, 0.6f), Triple(47.5f, 60.5f, 0.35f))
            .forEachIndexed { i, (from, to, alpha) ->
                val y = (59.5f + 9f * i) * u
                drawLine(
                    color = Color.White.copy(alpha = alpha),
                    start = Offset(from * u, y),
                    end = Offset(to * u, y),
                    strokeWidth = 5 * u,
                    cap = StrokeCap.Round,
                )
            }
    }
}
